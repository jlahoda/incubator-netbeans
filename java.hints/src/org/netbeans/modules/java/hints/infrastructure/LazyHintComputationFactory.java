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

package org.netbeans.modules.java.hints.infrastructure;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.JavaSource.Priority;
import org.netbeans.api.java.source.support.EditorAwareJavaSourceTaskFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Jan Lahoda
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.api.java.source.JavaSourceTaskFactory.class)
public class LazyHintComputationFactory extends EditorAwareJavaSourceTaskFactory {
    
    private static Map<FileObject, List<Reference<CreatorBasedLazyFixListBase>>> file2Creators = new WeakHashMap<>();
    
    /** Creates a new instance of LazyHintComputationFactory */
    public LazyHintComputationFactory() {
        super(Phase.RESOLVED, Priority.LOW);
    }

    public CancellableTask<CompilationInfo> createTask(FileObject file) {
        return new LazyHintComputation(file);
    }
 
    private static void rescheduleImpl(FileObject file) {
        LazyHintComputationFactory f = Lookup.getDefault().lookup(LazyHintComputationFactory.class);
        
        if (f != null) {
            f.reschedule(file);
        }
    }
    
    public static void addToCompute(FileObject file, CreatorBasedLazyFixListBase list) {
        synchronized (LazyHintComputationFactory.class) {
            List<Reference<CreatorBasedLazyFixListBase>> references = file2Creators.get(file);
            
            if (references == null) {
                file2Creators.put(file, references = new ArrayList<>());
            }
            
            references.add(new WeakReference(list));
        }
        
        rescheduleImpl(file);
    }
    
    public static synchronized List<CreatorBasedLazyFixListBase> getAndClearToCompute(FileObject file) {
        List<Reference<CreatorBasedLazyFixListBase>> references = file2Creators.get(file);
        
        if (references == null) {
            return Collections.emptyList();
        }
        
        List<CreatorBasedLazyFixListBase> result = new ArrayList<>();
        
        for (Reference<CreatorBasedLazyFixListBase> r : references) {
            CreatorBasedLazyFixListBase c = r.get();
            
            if (c != null) {
                result.add(c);
            }
        }
        
        references.clear();
        
        return result;
    }
}
