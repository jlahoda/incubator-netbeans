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
package org.netbeans.modules.java.source.remote.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**TODO: relies on get/putCachedValue for correctness (error fixes, etc.)
 *
 * @author lahvac
 */
public class Parser {

    private static int lastConfig;
    private static JavaSource source;

    public static <T> T parse(Config config, ParserTask<T> task) throws IOException {
        JavaSource source;
        
        if (lastConfig == config.id && Parser.source != null) {
            source = Parser.source;
        } else {
            FileObject root = FileUtil.createMemoryFileSystem().getRoot();
            FileObject file = FileUtil.createData(root, config.fileName);

            try (OutputStream out = file.getOutputStream();
                 Writer w = new OutputStreamWriter(out)) {
                w.append(config.fileContent);
            }

            ClasspathInfo cpInfo = ClasspathInfo.create(file);

            lastConfig = config.id;
            Parser.source = source = JavaSource.create(cpInfo, file);
        }

        Object[] result = new Object[1];
        source.runUserActionTask(new Task<CompilationController>() {
            @Override
            public void run(CompilationController cc) throws Exception {
                cc.toPhase(JavaSource.Phase.RESOLVED);
                result[0] = task.run(cc);
            }
        }, true);
        
        return (T) result[0];
    }
    
    public interface ParserTask<T> {
//        public T run(CompilationInfo info) throws Exception;
        public T run(CompilationController info) throws Exception; //XXX: CompilationController due to completion, allow?
    }

    public static final class Config {
        private static int nextId = 0; //XXX: synchronization

        public static Config create(CompilationInfo info) {
            Object conf = info.getCachedValue(Config.class);
            if (conf == null) {
                conf = new Config(nextId++, info.getFileObject().getNameExt(), info.getText());
                info.putCachedValue(Config.class, conf, CompilationInfo.CacheClearPolicy.ON_CHANGE);
            }
            return (Config) conf;
        }

        public static Config create(FileObject file) {
            //caching...
            try {
                return new Config(nextId++, file.getNameExt(), file.asText());
            } catch (IOException ex) {
                throw new IllegalStateException(ex); //XXX: error handling
            }
        }

        private int id;
        private String fileName;
        private String fileContent;
        //TODO: paths...

        private Config(int id, String fileName, String fileContent) {
            this.id = id;
            this.fileName = fileName;
            this.fileContent = fileContent;
        }

    }
    
}
