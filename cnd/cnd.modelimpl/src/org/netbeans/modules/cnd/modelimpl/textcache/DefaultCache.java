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

package org.netbeans.modules.cnd.modelimpl.textcache;

import org.netbeans.modules.cnd.utils.cache.APTStringManager;
import org.openide.util.CharSequences;

/**
 *
 */
public class DefaultCache extends APTStringManager {
    private static final APTStringManager manager = new DefaultCache();
    private DefaultCache() {
    }

    @Override
    public CharSequence getString(CharSequence text) {
        if (text == null){
            return text;
        }
        return CharSequences.create(text);
    }

    @Override
    public void dispose() {
    }

    public static APTStringManager getManager() {
        return manager;
    }
}
