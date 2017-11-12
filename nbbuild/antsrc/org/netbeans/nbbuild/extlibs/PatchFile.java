/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.nbbuild.extlibs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Patch task for the netbeans build process. Some packages need to be able
 * to apply diff patches while building and the ant patch task expects a native
 * patch binary to be present.
 * 
 * <p><strong>This task is not intended to stay and users should be updated
 * to use updated upstream packages.</strong></p>
 * 
 * <p>This file is mostly a verbatim copy of ContextualPatch from the diff
 * module, modified not to require netbeans modules and run stand-alone.</p>
 */
public class PatchFile extends Task {
    private String dir;
    private String patchfile;

    public String getDir() {
        return dir;
    }

    /**
     * @param dir the directory the patch is to be applied to
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getPatchfile() {
        return patchfile;
    }

    /**
     * @param patchfile Path to the patch file
     */
    public void setPatchfile(String patchfile) {
        this.patchfile = patchfile;
    }
    
    @Override
    public void execute() throws BuildException {
        try {
            File fPatchfile = new File(getProject().getBaseDir(), patchfile);
            File fDir = new File(getProject().getBaseDir(), dir);
            ContextualPatch cp = ContextualPatch.create(fPatchfile, fDir);
            
            boolean failed = false;
            for(ContextualPatch.PatchReport pr: cp.patch(false)) {
                getProject().log(String.format("[%s] %s", pr.status, pr.getFile()), Project.MSG_INFO);
                if(pr.getStatus() != ContextualPatch.PatchStatus.Patched) {
                    failed = true;
                    getProject().log("Failed: " + pr.getFailure());
                }
            }
            if(failed) {
                throw new BuildException("Patching failed");
            }
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
    }

    /**
     * Applies contextual patches to files. The patch file can contain patches
     * for multiple files.
     *
     * @author Maros Sandor
     */
    static class ContextualPatch {

        public static final String MAGIC = "# This patch file was generated by NetBeans IDE"; // NOI18N    

        // first seen in mercurial diffs: characters after the second @@ - ignore them  
        private final Pattern unifiedRangePattern = Pattern.compile("@@ -(\\d+)(,\\d+)? \\+(\\d+)(,\\d+)? @@(\\s.*)?");
        private final Pattern baseRangePattern = Pattern.compile("\\*\\*\\* (\\d+)(,\\d+)? \\*\\*\\*\\*");
        private final Pattern modifiedRangePattern = Pattern.compile("--- (\\d+)(,\\d+)? ----");
        private final Pattern normalChangeRangePattern = Pattern.compile("(\\d+)(,(\\d+))?c(\\d+)(,(\\d+))?");
        private final Pattern normalAddRangePattern = Pattern.compile("(\\d+)a(\\d+),(\\d+)");
        private final Pattern normalDeleteRangePattern = Pattern.compile("(\\d+),(\\d+)d(\\d+)");
        private final Pattern binaryHeaderPattern = Pattern.compile("MIME: (.*?); encoding: (.*?); length: (-?\\d+?)");

        private final File patchFile;
        private final File suggestedContext;

        private File context;
        private BufferedReader patchReader;
        private String patchLine;
        private boolean patchLineRead;
        private int lastPatchedLine;    // the last line that was successfuly patched

        public static ContextualPatch create(File patchFile, File context) {
            return new ContextualPatch(patchFile, context);
        }

        private ContextualPatch(File patchFile, File context) {
            this.patchFile = patchFile;
            this.suggestedContext = context;
        }

        /**
         *
         * @param dryRun true if the method should not make any modifications to
         *               files, false otherwise
         * @return
         * @throws PatchException
         * @throws IOException
         */
        public List<PatchReport> patch(boolean dryRun) throws PatchException, IOException {
            List<PatchReport> report = new ArrayList<>();
            init();
            try {
                patchLine = patchReader.readLine();
                List<SinglePatch> patches = new ArrayList<>();
                for (;;) {
                    SinglePatch patch = getNextPatch();
                    if (patch == null) {
                        break;
                    }
                    patches.add(patch);
                }
                computeContext(patches);

                // the patches must be resorted in order to correctly apply the changes
                // all copies/renames must take precedence before the actual modifications
                // of the source files. Because in copies and renames the original
                // file's content must not be changed before the copy takes place.
                resortPatches(patches);
                for (int i = 0; i < patches.size(); i++) {
                    SinglePatch patch = patches.get(i);
                    try {
                        applyPatch(patch, dryRun);
                        report.add(new PatchReport(patch.targetFile, computeBackup(patch.rename ? patch.sourceFile : patch.targetFile), patch.binary, PatchStatus.Patched, null));
                        patch.applied = true;
                    } catch (IOException e) {
                        report.add(new PatchReport(patch.targetFile, null, patch.binary, PatchStatus.Failure, e));
                    }
                }
                applyPendingDeletes(patches, report);
                return report;
            } finally {
                if (patchReader != null) {
                    try {
                        patchReader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        private void resortPatches(List<SinglePatch> patches) {
            Collections.sort(patches, new Comparator<SinglePatch>() {

                @Override
                public int compare(SinglePatch p1, SinglePatch p2) {
                    if (p1.copy && !p2.copy) {
                        return -1;
                    } else if (!p1.copy && p2.copy) {
                        return 1;
                    } else if (p1.rename && !p2.rename) {
                        return -1;
                    } else if (!p1.rename && p2.rename) {
                        return 1;
                    }
                    return 0;
                }
            });
        }

        private void applyPendingDeletes(List<SinglePatch> patches, List<PatchReport> report) throws IOException {
            for (SinglePatch patch : patches) {
                if (patch.rename && patch.applied) {
                    patch.sourceFile.delete();
                }
            }
        }

        private void init() throws IOException {
            patchReader = new BufferedReader(new FileReader(patchFile));
            String encoding = "ISO-8859-1";
            String line = patchReader.readLine();
            if (MAGIC.equals(line)) {
                encoding = "utf8"; // NOI18N
                line = patchReader.readLine();
            }
            patchReader.close();

            byte[] buffer = new byte[MAGIC.length()];
            
            try (InputStream in = new FileInputStream(patchFile)) {
                int read = in.read(buffer);
                if (read != -1 && MAGIC.equals(new String(buffer, "utf8"))) {  // NOI18N
                    encoding = "utf8"; // NOI18N
                }
            }
            patchReader = new BufferedReader(new InputStreamReader(new FileInputStream(patchFile), encoding));
        }

        private void applyPatch(SinglePatch patch, boolean dryRun) throws IOException, PatchException {
            lastPatchedLine = 1;
            List<String> target;
            patch.sourceFile = computeSourceFile(patch);
            patch.targetFile = computeTargetFile(patch);
            if (patch.sourceFile != null && patch.sourceFile.exists()) {
                if (patch.binary) {
                    target = new ArrayList<String>();
                } else {
                    target = readFile(patch.sourceFile);
                    if (patchCreatesNewFileThatAlreadyExists(patch, target)) {
                        return;
                    }
                }
            } else if (patch.targetFile.exists() && !patch.binary) {
                target = readFile(patch.targetFile);
                if (patchCreatesNewFileThatAlreadyExists(patch, target)) {
                    return;
                }
            } else {
                target = new ArrayList<>();
            }
            if (!patch.binary) {
                for (Hunk hunk : patch.hunks) {
                    applyHunk(target, hunk);
                }
            }
            if (!dryRun) {
                if (patch.sourceFile != null) {
                    backup(patch.sourceFile);
                }
                backup(patch.targetFile);
                writeFile(patch, target);
            }
        }

        private boolean patchCreatesNewFileThatAlreadyExists(SinglePatch patch, List<String> originalFile) throws PatchException {
            if (patch.hunks.length != 1) {
                return false;
            }
            Hunk hunk = patch.hunks[0];
            if (hunk.baseStart != 0 || hunk.baseCount != 0 || hunk.modifiedStart
                    != 1 || hunk.modifiedCount != originalFile.size()) {
                return false;
            }

            List<String> target = new ArrayList<>(hunk.modifiedCount);
            applyHunk(target, hunk);
            return target.equals(originalFile);
        }

        private void backup(File target) throws IOException {
            if (target.exists()) {
                copyStreamsCloseAll(new FileOutputStream(computeBackup(target)), new FileInputStream(target));
            }
        }

        private File computeBackup(File target) {
            return new File(target.getParentFile(), target.getName()
                    + ".original~");
        }

        private void copyStreamsCloseAll(OutputStream writer, InputStream reader) throws IOException {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
            writer.close();
            reader.close();
        }

        private void writeFile(SinglePatch patch, List<String> lines) throws IOException {
            patch.targetFile.getParentFile().mkdirs();
            if (patch.binary) {
                if (patch.hunks.length == 0) {
                    if (!patch.rename) {
                        // do not delete, it was a plain rename
                        patch.targetFile.delete();
                    }
                } else {
                    byte[] content = Base64.decode(patch.hunks[0].lines);
                    copyStreamsCloseAll(new FileOutputStream(patch.targetFile), new ByteArrayInputStream(content));
                }
            } else {
                Charset charset = getEncoding(patch.targetFile);
                try (PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(patch.targetFile), charset))) {
                    if (lines.isEmpty()) {
                        return;
                    }
                    for (String line : lines.subList(0, lines.size() - 1)) {
                        w.println(line);
                    }
                    w.print(lines.get(lines.size() - 1));
                    if (!patch.noEndingNewline) {
                        w.println();
                    }
                }
            }
        }

        private void applyHunk(List<String> target, Hunk hunk) throws PatchException {
            int idx = findHunkIndex(target, hunk);
            if (idx == -1) {
                throw new PatchException("Cannot apply hunk @@ "
                        + hunk.baseCount);
            }
            applyHunk(target, hunk, idx, false);
        }

        private int findHunkIndex(List<String> target, Hunk hunk) throws PatchException {
            int idx = hunk.modifiedStart;  // first guess from the hunk range specification
            if (idx >= lastPatchedLine && applyHunk(target, hunk, idx, true)) {
                return idx;
            } else {
                // try to search for the context
                for (int i = idx - 1; i >= lastPatchedLine; i--) {
                    if (applyHunk(target, hunk, i, true)) {
                        return i;
                    }
                }
                for (int i = idx + 1; i < target.size(); i++) {
                    if (applyHunk(target, hunk, i, true)) {
                        return i;
                    }
                }
            }
            return -1;
        }

        /**
         * @return true if the application succeeded
         */
        private boolean applyHunk(List<String> target, Hunk hunk, int idx, boolean dryRun) throws PatchException {
            idx--; // indices in the target list are 0-based
            for (String hunkLine : hunk.lines) {
                boolean isAddition = isAdditionLine(hunkLine);
                if (!isAddition) {
                    String targetLine = target.get(idx).trim();
                    if (!targetLine.equals(hunkLine.substring(1).trim())) { // be optimistic, compare trimmed context lines
                        if (dryRun) {
                            return false;
                        } else {
                            throw new PatchException("Unapplicable hunk @@ "
                                    + hunk.baseStart);
                        }
                    }
                }
                if (dryRun) {
                    if (isAddition) {
                        idx--;
                    }
                } else {
                    if (isAddition) {
                        target.add(idx, hunkLine.substring(1));
                    } else if (isRemovalLine(hunkLine)) {
                        target.remove(idx);
                        idx--;
                    }
                }
                idx++;
            }
            idx++; // indices in the target list are 0-based
            lastPatchedLine = idx;
            return true;
        }

        private boolean isAdditionLine(String hunkLine) {
            return hunkLine.charAt(0) == '+';
        }

        private boolean isRemovalLine(String hunkLine) {
            return hunkLine.charAt(0) == '-';
        }

        private Charset getEncoding(File file) {
            return Charset.forName("UTF-8");
        }

        private List<String> readFile(File target) throws IOException {
            try (InputStream is = new FileInputStream(target);
                    InputStreamReader isr = new InputStreamReader(is, getEncoding(target));
                    BufferedReader r = new BufferedReader(isr);
                    ) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = r.readLine()) != null) {
                    lines.add(line);
                }
                return lines;
            }
        }

        private SinglePatch getNextPatch() throws IOException, PatchException {
            SinglePatch patch = new SinglePatch();
            for (;;) {
                String line = readPatchLine();
                if (line == null) {
                    if (!patch.rename || patch.sourcePath == null
                            || patch.targetPath == null) {
                        patch = null;
                    }
                    break;
                }

                if (line.startsWith("Index:")) {
                    patch.targetPath = line.substring(6).trim();
                } else if (line.startsWith("MIME: application/octet-stream;")) {
                    unreadPatchLine();
                    readBinaryPatchContent(patch);
                    break;
                } else if (line.startsWith("--- ")) {
                    unreadPatchLine();
                    readPatchContent(patch);
                    break;
                } else if (line.startsWith("*** ")) {
                    unreadPatchLine();
                    readContextPatchContent(patch);
                    break;
                } else if (isNormalDiffRange(line)) {
                    unreadPatchLine();
                    readNormalPatchContent(patch);
                    break;
                } else if (line.startsWith("rename from ")) {
                    patch.sourcePath = line.substring(12);
                    patch.rename = true;
                } else if (line.startsWith("rename to ")) {
                    patch.targetPath = line.substring(10);
                } else if (line.startsWith("copy from ")) {
                    patch.sourcePath = line.substring(10);
                    patch.copy = true;
                } else if (line.startsWith("copy to ")) {
                    patch.targetPath = line.substring(8);
                }
            }
            return patch;
        }

        private boolean isNormalDiffRange(String line) {
            return normalAddRangePattern.matcher(line).matches()
                    || normalChangeRangePattern.matcher(line).matches()
                    || normalDeleteRangePattern.matcher(line).matches();
        }

        /**
         * Reads binary diff hunk.
         */
        private void readBinaryPatchContent(SinglePatch patch) throws PatchException, IOException {
            List<Hunk> hunks = new ArrayList<>();
            Hunk hunk = new Hunk();
            for (;;) {
                String line = readPatchLine();
                if (line == null || line.startsWith("Index:") || line.length()
                        == 0) {
                    unreadPatchLine();
                    break;
                }
                if (patch.binary) {
                    hunk.lines.add(line);
                } else {
                    Matcher m = binaryHeaderPattern.matcher(line);
                    if (m.matches()) {
                        patch.binary = true;
                        int length = Integer.parseInt(m.group(3));
                        if (length == -1) {
                            break;
                        }
                        hunks.add(hunk);
                    }
                }
            }
            patch.hunks = hunks.toArray(new Hunk[hunks.size()]);
        }

        /**
         * Reads normal diff hunks.
         */
        private void readNormalPatchContent(SinglePatch patch) throws IOException, PatchException {
            List<Hunk> hunks = new ArrayList<>();
            Hunk hunk = null;
            Matcher m;
            for (;;) {
                String line = readPatchLine();
                if (line == null || line.startsWith("Index:")) {
                    unreadPatchLine();
                    break;
                }
                if ((m = normalAddRangePattern.matcher(line)).matches()) {
                    hunk = new Hunk();
                    hunks.add(hunk);
                    parseNormalRange(hunk, m);
                } else if ((m = normalChangeRangePattern.matcher(line)).matches()) {
                    hunk = new Hunk();
                    hunks.add(hunk);
                    parseNormalRange(hunk, m);
                } else if ((m = normalDeleteRangePattern.matcher(line)).matches()) {
                    hunk = new Hunk();
                    hunks.add(hunk);
                    parseNormalRange(hunk, m);
                } else {
                    if (line.startsWith("> ")) {
                        hunk.lines.add("+" + line.substring(2));
                    } else if (line.startsWith("< ")) {
                        hunk.lines.add("-" + line.substring(2));
                    } else if (line.startsWith("---")) {
                        // ignore
                    } else {
                        throw new PatchException("Invalid hunk line: " + line);
                    }
                }
            }
            patch.hunks = hunks.toArray(new Hunk[hunks.size()]);
        }

        private void parseNormalRange(Hunk hunk, Matcher m) {
            if (m.pattern() == normalAddRangePattern) {
                hunk.baseStart = Integer.parseInt(m.group(1));
                hunk.baseCount = 0;
                hunk.modifiedStart = Integer.parseInt(m.group(2));
                hunk.modifiedCount = Integer.parseInt(m.group(3))
                        - hunk.modifiedStart + 1;
            } else if (m.pattern() == normalDeleteRangePattern) {
                hunk.baseStart = Integer.parseInt(m.group(1));
                hunk.baseCount = Integer.parseInt(m.group(2)) - hunk.baseStart
                        + 1;
                hunk.modifiedStart = Integer.parseInt(m.group(3));
                hunk.modifiedCount = 0;
            } else {
                hunk.baseStart = Integer.parseInt(m.group(1));
                if (m.group(3) != null) {
                    hunk.baseCount = Integer.parseInt(m.group(3))
                            - hunk.baseStart
                            + 1;
                } else {
                    hunk.baseCount = 1;
                }
                hunk.modifiedStart = Integer.parseInt(m.group(4));
                if (m.group(6) != null) {
                    hunk.modifiedCount = Integer.parseInt(m.group(6))
                            - hunk.modifiedStart + 1;
                } else {
                    hunk.modifiedCount = 1;
                }
            }
        }

        /**
         * Reads context diff hunks.
         */
        private void readContextPatchContent(SinglePatch patch) throws IOException, PatchException {
            String base = readPatchLine();
            if (base == null || !base.startsWith("*** ")) {
                throw new PatchException("Invalid context diff header: " + base);
            }
            String modified = readPatchLine();
            if (modified == null || !modified.startsWith("--- ")) {
                throw new PatchException("Invalid context diff header: "
                        + modified);
            }
            if (patch.targetPath == null) {
                computeTargetPath(base, modified, patch);
            }

            List<Hunk> hunks = new ArrayList<>();
            Hunk hunk = null;

            int lineCount = -1;
            for (;;) {
                String line = readPatchLine();
                if (line == null || line.length() == 0
                        || line.startsWith("Index:")) {
                    unreadPatchLine();
                    break;
                } else if (line.startsWith("***************")) {
                    hunk = new Hunk();
                    parseContextRange(hunk, readPatchLine());
                    hunks.add(hunk);
                } else if (line.startsWith("--- ")) {
                    lineCount = 0;
                    parseContextRange(hunk, line);
                    hunk.lines.add(line);
                } else {
                    char c = line.charAt(0);
                    if (c == ' ' || c == '+' || c == '-' || c == '!') {
                        if (lineCount < hunk.modifiedCount) {
                            hunk.lines.add(line);
                            if (lineCount != -1) {
                                lineCount++;
                            }
                        }
                    } else {
                        throw new PatchException("Invalid hunk line: " + line);
                    }
                }
            }
            patch.hunks = hunks.toArray(new Hunk[hunks.size()]);
            convertContextToUnified(patch);
        }

        private void convertContextToUnified(SinglePatch patch) throws PatchException {
            Hunk[] unifiedHunks = new Hunk[patch.hunks.length];
            int idx = 0;
            for (Hunk hunk : patch.hunks) {
                unifiedHunks[idx++] = convertContextToUnified(hunk);
            }
            patch.hunks = unifiedHunks;
        }

        private Hunk convertContextToUnified(Hunk hunk) throws PatchException {
            Hunk unifiedHunk = new Hunk();
            unifiedHunk.baseStart = hunk.baseStart;
            unifiedHunk.modifiedStart = hunk.modifiedStart;
            int split = -1;
            for (int i = 0; i < hunk.lines.size(); i++) {
                if (hunk.lines.get(i).startsWith("--- ")) {
                    split = i;
                    break;
                }
            }
            if (split == -1) {
                throw new PatchException("Missing split divider in context patch");
            }

            int baseIdx = 0;
            int modifiedIdx = split + 1;
            List<String> unifiedLines = new ArrayList<>(hunk.lines.size());
            for (; baseIdx < split || modifiedIdx < hunk.lines.size();) {
                String baseLine = baseIdx < split ? hunk.lines.get(baseIdx) : "~";
                String modifiedLine = modifiedIdx < hunk.lines.size() ? hunk.lines.get(modifiedIdx) : "~";
                if (baseLine.startsWith("- ")) {
                    unifiedLines.add("-" + baseLine.substring(2));
                    unifiedHunk.baseCount++;
                    baseIdx++;
                } else if (modifiedLine.startsWith("+ ")) {
                    unifiedLines.add("+" + modifiedLine.substring(2));
                    unifiedHunk.modifiedCount++;
                    modifiedIdx++;
                } else if (baseLine.startsWith("! ")) {
                    unifiedLines.add("-" + baseLine.substring(2));
                    unifiedHunk.baseCount++;
                    baseIdx++;
                } else if (modifiedLine.startsWith("! ")) {
                    unifiedLines.add("+" + modifiedLine.substring(2));
                    unifiedHunk.modifiedCount++;
                    modifiedIdx++;
                } else if (baseLine.startsWith("  ")
                        && modifiedLine.startsWith("  ")) {
                    unifiedLines.add(baseLine.substring(1));
                    unifiedHunk.baseCount++;
                    unifiedHunk.modifiedCount++;
                    baseIdx++;
                    modifiedIdx++;
                } else if (baseLine.startsWith("  ")) {
                    unifiedLines.add(baseLine.substring(1));
                    unifiedHunk.baseCount++;
                    unifiedHunk.modifiedCount++;
                    baseIdx++;
                } else if (modifiedLine.startsWith("  ")) {
                    unifiedLines.add(modifiedLine.substring(1));
                    unifiedHunk.baseCount++;
                    unifiedHunk.modifiedCount++;
                    modifiedIdx++;
                } else {
                    throw new PatchException("Invalid context patch: "
                            + baseLine);
                }
            }
            unifiedHunk.lines = unifiedLines;
            return unifiedHunk;
        }

        /**
         * Reads unified diff hunks.
         */
        private void readPatchContent(SinglePatch patch) throws IOException, PatchException {
            String base = readPatchLine();
            if (base == null || !base.startsWith("--- ")) {
                throw new PatchException("Invalid unified diff header: " + base);
            }
            String modified = readPatchLine();
            if (modified == null || !modified.startsWith("+++ ")) {
                throw new PatchException("Invalid unified diff header: "
                        + modified);
            }
            if (patch.targetPath == null) {
                computeTargetPath(base, modified, patch);
            }

            List<Hunk> hunks = new ArrayList<>();
            Hunk hunk = null;

            for (;;) {
                String line = readPatchLine();
                if (line == null || line.length() == 0
                        || line.startsWith("Index:")) {
                    unreadPatchLine();
                    break;
                }
                char c = line.charAt(0);
                if (c == '@') {
                    hunk = new Hunk();
                    parseRange(hunk, line);
                    hunks.add(hunk);
                } else if (c == ' ' || c == '+' || c == '-') {
                    hunk.lines.add(line);
                } else if (line.equals(Hunk.ENDING_NEWLINE)) {
                    patch.noEndingNewline = true;
                } else {
                    // first seen in mercurial diffs: be optimistic, this is probably the end of this patch  
                    unreadPatchLine();
                    break;
                }
            }
            patch.hunks = hunks.toArray(new Hunk[hunks.size()]);
        }

        private void computeTargetPath(String base, String modified, SinglePatch patch) {
            base = base.substring("+++ ".length());
            modified = modified.substring("--- ".length());
            // first seen in mercurial diffs: base and modified paths are different: base starts with "a/" and modified starts with "b/"
            if (base.startsWith("a/") && modified.startsWith("b/")) {
                base = base.substring(2);
            } else if (base.startsWith("/dev/null") && modified.startsWith("b/")) {
                modified = modified.substring(2);
            } else if (base.startsWith("a/") && modified.startsWith("/dev/null")) {
                base = base.substring(2);
            }

            // base /dev/null => new file
            // modified /dev/null => deleted file
            String target = base.startsWith("/dev/null") ? modified : base;

            int pathEndIdx = target.indexOf('\t');
            if (pathEndIdx == -1) {
                pathEndIdx = target.length();
            }
            patch.targetPath = target.substring(0, pathEndIdx).trim();
        }

        private void parseRange(Hunk hunk, String range) throws PatchException {
            Matcher m = unifiedRangePattern.matcher(range);
            if (!m.matches()) {
                throw new PatchException("Invalid unified diff range: " + range);
            }
            hunk.baseStart = Integer.parseInt(m.group(1));
            hunk.baseCount = m.group(2) != null ? Integer.parseInt(m.group(2).substring(1)) : 1;
            hunk.modifiedStart = Integer.parseInt(m.group(3));
            hunk.modifiedCount = m.group(4) != null ? Integer.parseInt(m.group(4).substring(1)) : 1;
        }

        private void parseContextRange(Hunk hunk, String range) throws PatchException {
            if (range.charAt(0) == '*') {
                Matcher m = baseRangePattern.matcher(range);
                if (!m.matches()) {
                    throw new PatchException("Invalid context diff range: "
                            + range);
                }
                hunk.baseStart = Integer.parseInt(m.group(1));
                hunk.baseCount = m.group(2) != null ? Integer.parseInt(m.group(2).substring(1)) : 1;
                hunk.baseCount -= hunk.baseStart - 1;
            } else {
                Matcher m = modifiedRangePattern.matcher(range);
                if (!m.matches()) {
                    throw new PatchException("Invalid context diff range: "
                            + range);
                }
                hunk.modifiedStart = Integer.parseInt(m.group(1));
                hunk.modifiedCount = m.group(2) != null ? Integer.parseInt(m.group(2).substring(1)) : 1;
                hunk.modifiedCount -= hunk.modifiedStart - 1;
            }
        }

        private String readPatchLine() throws IOException {
            if (patchLineRead) {
                patchLine = patchReader.readLine();
            } else {
                patchLineRead = true;
            }
            return patchLine;
        }

        private void unreadPatchLine() {
            patchLineRead = false;
        }

        private void computeContext(List<SinglePatch> patches) {
            File bestContext = suggestedContext;
            int bestContextMatched = 0;
            for (context = suggestedContext; context != null; context = context.getParentFile()) {
                int patchedFiles = 0;
                for (SinglePatch patch : patches) {
                    try {
                        applyPatch(patch, true);
                        patchedFiles++;
                    } catch (IOException e) {
                        // patch failed to apply
                    }
                }
                if (patchedFiles > bestContextMatched) {
                    bestContextMatched = patchedFiles;
                    bestContext = context;
                    if (patchedFiles == patches.size()) {
                        break;
                    }
                }
            }
            context = bestContext;
        }

        private File computeSourceFile(SinglePatch patch) {
            if (patch.sourcePath == null) {
                return null;
            }
            if (context.isFile()) {
                return context;
            }
            return new File(context, patch.sourcePath);
        }

        private File computeTargetFile(SinglePatch patch) {
            if (patch.targetPath == null) {
                patch.targetPath = context.getAbsolutePath();
            }
            if (context.isFile()) {
                return context;
            }
            return new File(context, patch.targetPath);
        }

        private class SinglePatch {

            String targetPath;
            Hunk[] hunks = new Hunk[0];
            boolean targetMustExist = true;     // == false if the patch contains one hunk with just additions ('+' lines)
            File targetFile;                 // computed later
            boolean noEndingNewline;            // resulting file should not end with a newline
            boolean binary;                  // binary patches contain one encoded Hunk
            boolean copy;
            boolean rename;
            String sourcePath;
            File sourceFile;                 // computed later
            boolean applied;
        }

        public static enum PatchStatus {
            Patched, Missing, Failure
        };

        public static final class PatchReport {

            private final File file;
            private final File originalBackupFile;
            private final boolean binary;
            private final PatchStatus status;
            private final Throwable failure;

            PatchReport(File file, File originalBackupFile, boolean binary, PatchStatus status, Throwable failure) {
                this.file = file;
                this.originalBackupFile = originalBackupFile;
                this.binary = binary;
                this.status = status;
                this.failure = failure;
            }

            public File getFile() {
                return file;
            }

            public File getOriginalBackupFile() {
                return originalBackupFile;
            }

            public boolean isBinary() {
                return binary;
            }

            public PatchStatus getStatus() {
                return status;
            }

            public Throwable getFailure() {
                return failure;
            }
        }

        /**
         * The patch is invalid or cannot be applied on the specified file.
         *
         * @author Maros Sandor
         */
        static final class PatchException extends IOException {

            public PatchException(String msg) {
                super(msg);
            }
        }

        static final class Hunk {

            public static final String ENDING_NEWLINE = "\\ No newline at end of file";

            public int baseStart;
            public int baseCount;
            public int modifiedStart;
            public int modifiedCount;
            public List<String> lines = new ArrayList<>();
        }

        static class Base64 {

            private Base64() {
            }

            public static byte[] decode(List<String> ls) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                for (String s : ls) {
                    decode(s, bos);
                }
                return bos.toByteArray();
            }

            private static void decode(String s, ByteArrayOutputStream bos) {
                int i = 0;
                int len = s.length();
                while (true) {
                    while (i < len && s.charAt(i) <= ' ') {
                        i++;
                    }
                    if (i == len) {
                        break;
                    }
                    int tri = (decode(s.charAt(i)) << 18)
                            + (decode(s.charAt(i + 1)) << 12)
                            + (decode(s.charAt(i + 2)) << 6)
                            + (decode(s.charAt(i + 3)));

                    bos.write((tri >> 16) & 255);
                    if (s.charAt(i + 2) == '=') {
                        break;
                    }
                    bos.write((tri >> 8) & 255);
                    if (s.charAt(i + 3) == '=') {
                        break;
                    }
                    bos.write(tri & 255);

                    i += 4;
                }
            }

            private static int decode(char c) {
                if (c >= 'A' && c <= 'Z') {
                    return ((int) c) - 65;
                } else if (c >= 'a' && c <= 'z') {
                    return ((int) c) - 97 + 26;
                } else if (c >= '0' && c <= '9') {
                    return ((int) c) - 48 + 26 + 26;
                } else {
                    switch (c) {
                        case '+':
                            return 62;
                        case '/':
                            return 63;
                        case '=':
                            return 0;
                        default:
                            throw new RuntimeException("unexpected code: " + c);
                    }
                }
            }

        }
    }
}
