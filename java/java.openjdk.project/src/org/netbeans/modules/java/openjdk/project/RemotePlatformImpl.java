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
package org.netbeans.modules.java.openjdk.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.java.openjdk.project.JDKProject.Root;
import org.netbeans.modules.java.openjdk.project.JDKProject.RootKind;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author lahvac
 */
public class RemotePlatformImpl {//implements RemotePlatform {

    private static final Map<FileObject, Object/*should be: RemotePlatformImpl*/> jdkRoot2Platform = new HashMap<>();

    public static @NonNull Object/*should be: RemotePlatformImpl*/ getProvider(FileObject jdkRoot, ConfigurationImpl.ProviderImpl configurations) {
        return jdkRoot2Platform.computeIfAbsent(jdkRoot, r -> {
            ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
            if (cl == null)
                return null;
            try {
                Class<?> remotePlatform = cl.loadClass("org.netbeans.modules.java.source.remote.spi.RemotePlatform");
                RemotePlatformImpl delegate = new RemotePlatformImpl(configurations);
                return Proxy.newProxyInstance(cl,
                                       new Class<?>[] {remotePlatform},
                                       (inst, meth, params) -> {
                    return delegate.getClass()
                                   .getMethod(meth.getName(), meth.getParameterTypes())
                                   .invoke(delegate, params);
                });
            } catch (ClassNotFoundException ex) {
                //OK:
                return null;
            }
        });
    }

    private final ChangeSupport cs = new ChangeSupport(this);
    private final ConfigurationImpl.ProviderImpl configurations;

    public RemotePlatformImpl(ConfigurationImpl.ProviderImpl configurations) {
        this.configurations = configurations;
        this.configurations.addPropertyChangeListener(new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) {
                if (ProjectConfigurationProvider.PROP_CONFIGURATION_ACTIVE.equals(evt.getPropertyName()) ||
                    evt.getPropertyName() == null) {
                    cs.fireChange();
                }
            }
        });
    }

//    @Override
    public String getJavaCommand() {
        return new File(configurations.getActiveConfiguration().getLocation(),
                        "images/jdk/bin/java".replace("/", System.getProperty("file.separator")))
               .getAbsolutePath();
    }

//    @Override
    public List<String> getJavaArguments() {
        return Collections.emptyList();
    }

//    @Override
    public void addChangeListener(ChangeListener l) {
        cs.addChangeListener(l);
    }

//    @Override
    public void removeChangeListener(ChangeListener l) {
        cs.removeChangeListener(l);
    }

    public static Lookup/*should be: RemotePlatform.Provider*/ createProvider(FileObject jdkRoot, JDKProject project) {
        Object/*should be: RemotePlatformImpl*/ platform = RemotePlatformImpl.getProvider(jdkRoot, project.configurations);
        ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
        if (cl == null)
            return Lookup.EMPTY;
        try {
            Class<?> remotePlatformProvider = cl.loadClass("org.netbeans.modules.java.source.remote.spi.RemotePlatform$Provider");
            return Lookups.singleton(Proxy.newProxyInstance(cl,
                                   new Class<?>[] {remotePlatformProvider},
                                   (inst, meth, params) -> {
                switch (meth.getName()) {
                    case "findPlatform":
                        switch (project.getLookup().lookup(Settings.class).getUseRemotePlatform()) {
                            case TEST:
                                FileObject file = (FileObject) params[0];
                                String fileURL = file.toURL().toString();
                                //TODO: more reliable tests detection?
                                boolean tests = false;
                                for (Root root : project.getRoots()) {
                                    if (root.kind != RootKind.TEST_SOURCES)
                                        continue;
                                    tests |= fileURL.startsWith(root.getLocation().toString());
                                }
                                if (!tests) {
                                    return null;
                                }
                            case ALWAYS:
                                return platform;
                            case NEVER: return null;
                        }
                    default:
                        return null;
                }
            }));
        } catch (ClassNotFoundException ex) {
            return Lookup.EMPTY;
        }
    }

}