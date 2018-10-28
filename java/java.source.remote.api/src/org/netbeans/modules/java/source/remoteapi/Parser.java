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
package org.netbeans.modules.java.source.remoteapi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.java.queries.CompilerOptionsQuery;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.java.source.usages.ClasspathInfoAccessor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**TODO: relies on get/putCachedValue for correctness (error fixes, etc.)
 *
 * @author lahvac
 */
public class Parser {

    private static Map<String, Reference<FileObject>> location2FileObject = new HashMap<>();

    public static <T> T runTask(Config config, ParserTask<T, CompilationInfo> task) throws IOException {
        return runControllerTask(config, cc -> {
            cc.toPhase(JavaSource.Phase.RESOLVED);
            return task.run(cc);
        });
    }

    public static <T> T runControllerTask(Config config, ParserTask<T, CompilationController> task) throws IOException {
        Reference<FileObject> foRef = location2FileObject.get(config.fileUri);
        FileObject file = foRef != null ? foRef.get() : null;

        if (file == null) {
            FileObject root = FileUtil.createMemoryFileSystem().getRoot();
            file = FileUtil.createData(root, config.fileName);

            location2FileObject.put(config.fileUri, new SoftReference<>(file));
        }

        file.setAttribute(SourceLevelQueryImpl.KEY_SOURCE_LEVEL, config.sourceLevel);
        file.setAttribute(CompilerOptionsQueryImpl.KEY_COMPILER_OPTIONS, config.compilerOptions);

        if (!file.asText().equals(config.fileContent)) {
            try (OutputStream out = file.getOutputStream();
                 Writer w = new OutputStreamWriter(out)) {
                w.append(config.fileContent);
            }
        }

        ClasspathInfo cpInfo = ClasspathInfoAccessor.getINSTANCE().deserialize(config.cpInfo);

        JavaSource source = JavaSource.create(cpInfo, file);

        Object[] result = new Object[1];
        source.runUserActionTask(new Task<CompilationController>() {
            @Override
            public void run(CompilationController cc) throws Exception {
                result[0] = task.run(cc);
            }
        }, true);
        
        return (T) result[0];
    }
    
    public interface ParserTask<T, CI extends CompilationInfo> {
        public T run(CI info) throws Exception;
    }

    public static final class Config {
        private static int nextId = 0; //XXX: synchronization

        public static Config create(CompilationInfo info) {
            Object conf = info.getCachedValue(Config.class);
            if (conf == null) {
                conf = new Config(nextId++, info.getFileObject().getNameExt(), info.getFileObject().toURI().toString(), info.getText(), info.getClasspathInfo(), SourceLevelQuery.getSourceLevel(info.getFileObject()), CompilerOptionsQuery.getOptions(info.getFileObject()).getArguments());
                info.putCachedValue(Config.class, conf, CompilationInfo.CacheClearPolicy.ON_CHANGE);
            }
            return (Config) conf;
        }

        public static Config create(FileObject file) {
            //caching...
            try {
                EditorCookie ec = file.getLookup().lookup(EditorCookie.class);
                StyledDocument doc = ec != null ? ec.getDocument() : null;
                String[] text = new String[1];
                
                if (doc != null) {
                    doc.render(() -> {
                        try {
                            text[0] = doc.getText(0, doc.getLength());
                        } catch (BadLocationException ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
                } else {
                    text[0] = file.asText();
                }
                return new Config(nextId++, file.getNameExt(), file.toURI().toString(), text[0], ClasspathInfo.create(file), SourceLevelQuery.getSourceLevel(file), CompilerOptionsQuery.getOptions(file).getArguments());
            } catch (IOException ex) {
                throw new IllegalStateException(ex); //XXX: error handling
            }
        }

        private int id;
        private String fileName;
        private String fileUri;
        private String fileContent;
        private Map<String, Object> cpInfo;
        private String sourceLevel;
        private List<? extends String> compilerOptions;

        public Config(int id, String fileName, String fileUri, String fileContent,
                      ClasspathInfo cpInfo, String sourceLevel, List<? extends String> compilerOptions) {
            this.id = id;
            this.fileName = fileName;
            this.fileUri = fileUri;
            this.fileContent = fileContent;
            this.cpInfo = ClasspathInfoAccessor.getINSTANCE().serialize(cpInfo);
            this.sourceLevel = sourceLevel;
            this.compilerOptions = compilerOptions;
        }

    }
    
}