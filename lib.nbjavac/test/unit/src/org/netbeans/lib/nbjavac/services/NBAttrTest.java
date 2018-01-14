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
package org.netbeans.lib.nbjavac.services;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.netbeans.junit.NbTestCase;

import static org.netbeans.lib.nbjavac.services.Utilities.DEV_NULL;
import org.openide.util.Pair;

/**
 *
 * @author lahvac
 */
public class NBAttrTest extends NbTestCase {

    public NBAttrTest(String testName) {
        super(testName);
    }

    public void testNETBEANS_228() throws Exception {
        String code = "package test; public class Test { Object t() { return new Undef() { public void test() { test(); } }; } }";
        Pair<JavacTask, CompilationUnitTree> parsed = compile(code);

        new TreePathScanner<Void, Void>() {
            public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
                Trees trees = Trees.instance(parsed.first());
                assertNotNull(trees.getElement(getCurrentPath()));
                return super.visitMethodInvocation(tree, p);
            }
        }.scan(parsed.second(), null);
    }

    //<editor-fold defaultstate="collapsed" desc=" Test Infrastructure ">
    private static class MyFileObject extends SimpleJavaFileObject {
        private String text;

        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    private File workingDir;

    @Override
    protected void setUp() throws Exception {
        workingDir = getWorkDir();
    }

    private Pair<JavacTask, CompilationUnitTree> compile(String code) throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        StandardJavaFileManager std = tool.getStandardFileManager(null, null, null);

        std.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(workingDir));

        Context context = new Context();
        NBMessager.preRegister(context, null, DEV_NULL, DEV_NULL, DEV_NULL);
        NBAttr.preRegister(context);
        final JavacTaskImpl ct = (JavacTaskImpl) ((JavacTool)tool).getTask(null, std, null, Arrays.asList("-source", "1.6", "-target", "1.6"), null, Arrays.asList(new MyFileObject(code)), context);

        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();

        return Pair.<JavacTask, CompilationUnitTree>of(ct, cut);
    }
    //</editor-fold>
}
