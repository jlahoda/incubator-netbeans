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

package org.netbeans.tests.j2eeserver.plugin;

import org.netbeans.modules.j2ee.deployment.common.api.ConfigurationException;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.plugins.spi.config.ModuleConfiguration;
import org.netbeans.modules.j2ee.deployment.plugins.spi.config.ModuleConfigurationFactory;
import org.openide.util.Lookup;

/**
 *
 * @author Petr Hejl
 */
public class TestModuleConfigurationFactory implements ModuleConfigurationFactory {

    private static final TestModuleConfigurationFactory INSTANCE = new TestModuleConfigurationFactory();

    private TestModuleConfigurationFactory() {
        super();
    }

    public static TestModuleConfigurationFactory getInstance() {
        return INSTANCE;
    }

    public ModuleConfiguration create(final J2eeModule j2eeModule) throws ConfigurationException {
        return new ModuleConfiguration() {

            {
                // EAR tests require this
                j2eeModule.getModuleVersion();
                j2eeModule.getType();
            }

            public Lookup getLookup() {
                return Lookup.EMPTY;
            }

            public J2eeModule getJ2eeModule() {
                return j2eeModule;
            }

            public void dispose() {
                // noop
            }
        };
    }

}
