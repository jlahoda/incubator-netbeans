/*
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

package org.netbeans.modules.git.remote.ui.diff;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.diff.Difference;
import org.netbeans.api.diff.StreamSource;
import org.netbeans.modules.git.remote.Git;
import org.netbeans.modules.git.remote.VersionsCache;
import org.netbeans.modules.git.remote.utils.GitUtils;
import org.netbeans.modules.remotefs.versioning.api.VCSFileProxySupport;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.loaders.MultiDataObject;
import org.openide.nodes.CookieSet;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 */
public class DiffStreamSource extends StreamSource {

    private final VCSFileProxy baseFile;
    private final String revision;
    private final String title;
    private String mimeType;
    private Boolean start;

    /**
     * Null is a valid value if base file does not exist in this revision.
     */
    private VCSFileProxy remoteFile;
    private Boolean canWriteBaseFile;
    private final VCSFileProxy fileInRevision;

    /**
     * Creates a new StreamSource implementation for Diff engine.
     *
     * @param baseFile
     * @param revision file revision, may be null if the revision does not exist (ie for new files)
     * @param title title to use in diff panel
     */
    public DiffStreamSource (VCSFileProxy fileInRevision, VCSFileProxy baseFile, String revision, String title) {
        this.baseFile = baseFile;
        this.fileInRevision = fileInRevision;
        this.revision = revision;
        this.title = title;
        this.start = true;
    }

    @Override
    public String getName() {
        if (fileInRevision != null) {
            VCSFileProxy repo = Git.getInstance().getRepositoryRoot(fileInRevision);
            if (repo != null) {
                return GitUtils.getRelativePath(repo, fileInRevision);
            } else {
                return fileInRevision.getName();
            }
        } else {
            return NbBundle.getMessage(DiffStreamSource.class, "LBL_Diff_Anonymous"); // NOI18N
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public synchronized String getMIMEType() {
        try {
            init();
        } catch (IOException e) {
            return null;
        }
        return mimeType;
    }

    @Override
    public synchronized Reader createReader() throws IOException {
        init();
        if (revision == null || remoteFile == null) {
            return null;
        }
        if (!mimeType.startsWith("text/")) { // NOI18N
            return null;
        } else {
            return Utils.createReader(remoteFile.toFileObject());
        }
    }

    @Override
    public Writer createWriter (Difference[] conflicts) throws IOException {
        throw new IOException("Operation not supported"); // NOI18N
    }

    @Override
    public boolean isEditable () {
        return GitUtils.CURRENT.equals(revision) && isPrimary() && isBaseFileWritable();
    }

    private boolean isBaseFileWritable () {
        if (canWriteBaseFile == null) {
            FileObject fo = baseFile.toFileObject();
            canWriteBaseFile = fo != null && fo.canWrite();
        }
        return canWriteBaseFile;
    }

    private boolean isPrimary() {
        FileObject fo = baseFile.toFileObject();
        if (fo != null) {
            try {
                DataObject dao = DataObject.find(fo);
                return fo.equals(dao.getPrimaryFile());
            } catch (DataObjectNotFoundException e) {
                // no dataobject, never mind
            }
        }
        return true;
    }

    @Override
    public synchronized Lookup getLookup() {
        try {
            init();
        } catch (IOException e) {
            return Lookups.fixed();
        }
        if (remoteFile == null || !isPrimary()) {
            return Lookups.fixed();
        }
        FileObject remoteFo = remoteFile.toFileObject();
        if (remoteFo == null) {
            return Lookups.fixed();
        }

        return Lookups.fixed(remoteFo);
    }

    /**
     * Loads data.
     */
    synchronized void init() throws IOException {
        if (baseFile.isDirectory()) {
            return;
        }
        if (start == false) {
            return;
        }
        start = false;
        if (remoteFile != null || revision == null) {
            return;
        }
        mimeType = GitUtils.getMimeType(baseFile);
        try {
            if (isEditable()) {
                // we cannot move editable documents because that would break Document sharing
                remoteFile = VersionsCache.getInstance().getFileRevision(baseFile, revision, GitUtils.NULL_PROGRESS_MONITOR);
            } else {
                VCSFileProxy tempFolder = VCSFileProxySupport.getTempFolder(baseFile, true);
                // To correctly get content of the base file, we need to checkout all files that belong to the same
                // DataObject. One example is Form files: data loader removes //GEN:BEGIN comments from the java file but ONLY
                // if it also finds associate .form file in the same directory
                Set<VCSFileProxy> allFiles = VCSFileProxySupport.getAllDataObjectFiles(baseFile);
                Map<VCSFileProxy, VCSFileProxy> allFilePairs = new HashMap<>(allFiles.size());
                boolean renamed = !baseFile.equals(fileInRevision);
                for (VCSFileProxy f : allFiles) {
                    if (renamed) {
                        allFilePairs.put(renameFile(f, baseFile, fileInRevision), f);
                    } else {
                        allFilePairs.put(f, f);
                    }
                }
                for (Map.Entry<VCSFileProxy, VCSFileProxy> entry : allFilePairs.entrySet()) {
                    VCSFileProxy file = entry.getKey();
                    VCSFileProxy currentPair = entry.getValue();
                    boolean isBase = file.equals(fileInRevision);
                    try {
                        VCSFileProxy rf = VersionsCache.getInstance().getFileRevision(file, revision, GitUtils.NULL_PROGRESS_MONITOR);
                        if (rf == null) {
                            remoteFile = null;
                            return;
                        }
                        VCSFileProxy newRemoteFile = VCSFileProxy.createFileProxy(tempFolder, file.getName());
                        Utils.copyStreamsCloseAll(VCSFileProxySupport.getOutputStream(newRemoteFile), rf.getInputStream(false));
                        VCSFileProxySupport.deleteOnExit(newRemoteFile);
                        
                        if (isBase) {
                            remoteFile = newRemoteFile;
                            VCSFileProxy encodingHolder = currentPair;
                            if (encodingHolder.exists()) {
                                VCSFileProxySupport.associateEncoding(encodingHolder, newRemoteFile);
                            } else if (remoteFile != null) {
                                boolean created = false;
                                try {
                                    if (encodingHolder.getParentFile().exists()) {
                                        created = VCSFileProxySupport.createNew(encodingHolder);
                                        VCSFileProxySupport.associateEncoding(encodingHolder, newRemoteFile);
                                    }
                                } catch (IOException ex) {
                                    // not interested
                                } finally {
                                    if (created) {
                                        VCSFileProxySupport.delete(encodingHolder);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        if (isBase) {
                            throw e;
                        }
                        // we cannot check out peer file so the dataobject will not be constructed properly
                    }
                }
            }
            if (!baseFile.exists() && remoteFile != null && remoteFile.exists()) {
                mimeType = GitUtils.getMimeType(remoteFile);
            }
        } catch (Exception e) {
            throw new IOException("Can not load remote file for " + baseFile, e);
        }
    }

    @Override
    public void close () {
        EditorCookie.Observable ec = getEditableCookie(remoteFile);
        if (ec != null && ec.getOpenedPanes() == null && !ec.isModified()) {
            ec.close();
        }
        super.close();
    }
    
    private static EditorCookie.Observable getEditableCookie (VCSFileProxy file) {
        EditorCookie.Observable editorCookie = null;
        if (file == null) {
            return null;
        }
        FileObject fileObj = file.toFileObject();
        if (fileObj != null) {
            try {
                DataObject dao = DataObject.find(fileObj);
                if (dao instanceof MultiDataObject) {
                    MultiDataObject mdao = (MultiDataObject) dao;
                    for (MultiDataObject.Entry entry : mdao.secondaryEntries()) {
                        if (fileObj == entry.getFile() && entry instanceof CookieSet.Factory) {
                            CookieSet.Factory factory = (CookieSet.Factory) entry;
                            EditorCookie ec = factory.createCookie(EditorCookie.class);
                            if (ec instanceof EditorCookie.Observable) {
                                editorCookie = (EditorCookie.Observable) ec;
                            }
                        }
                    }
                }
                if (editorCookie == null) {
                    EditorCookie cookie = dao.getCookie(EditorCookie.class);
                    if (cookie instanceof EditorCookie.Observable) {
                        editorCookie = (EditorCookie.Observable) cookie;
                    }
                }
            } catch (DataObjectNotFoundException ex) {
            }
        }
        return editorCookie;
    }

    private VCSFileProxy renameFile (VCSFileProxy toRename, VCSFileProxy baseFile, VCSFileProxy renamedBaseFile) {
        VCSFileProxy parent = renamedBaseFile.getParentFile();
        String baseFileName = baseFile.getName();
        String renamedFileName = renamedBaseFile.getName();
        String toRenameFileName = toRename.getName();
        String retval = toRenameFileName;
        if (!renamedFileName.equals(baseFileName)) {
            String baseNameNoExt = getFileNameNoExt(baseFileName);
            String renamedNameNoExt = getFileNameNoExt(renamedFileName);
            if (toRenameFileName.startsWith(baseNameNoExt)) {
                retval = renamedNameNoExt;
                if (toRenameFileName.length() > baseNameNoExt.length()) {
                    retval += toRenameFileName.substring(baseNameNoExt.length());
                }
            }
        }
        return VCSFileProxy.createFileProxy(parent, retval);
    }

    private String getFileNameNoExt (String fileName) {
        int pos = fileName.lastIndexOf('.');
        if (pos != -1) {
            return fileName.substring(0, pos);
        }
        return fileName;
    }
}
