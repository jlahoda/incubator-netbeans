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

package org.netbeans.modules.java.editor.hyperlink;

import java.util.EnumSet;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.modules.editor.NbEditorDocument;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.editor.java.GoToSupport;
import org.netbeans.modules.java.editor.overridden.GoToImplementation;
import org.netbeans.spi.java.source.RemotePlatform;
import org.openide.filesystems.FileObject;

/**
 * Implementation of the hyperlink provider for java language.
 * <br>
 * The hyperlinks are constructed for identifiers.
 * <br>
 * The click action corresponds to performing the goto-declaration action.
 *
 * @author Jan Lahoda
 */
public final class JavaHyperlinkProvider implements HyperlinkProviderExt {
 
    public JavaHyperlinkProvider() {
    }

    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION, HyperlinkType.ALT_HYPERLINK);
    }

    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
        return getHyperlinkSpan(doc, offset, type) != null;
    }

    public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
        FileObject file = NbEditorUtilities.getFileObject(doc);
        if (file != null && RemotePlatform.hasRemotePlatform(file)) {
            return null;
        }
        return GoToSupport.getIdentifierOrLambdaArrowSpan(doc, offset, null);
    }

    public void performClickAction(Document doc, int offset, HyperlinkType type) {
        switch (type) {
            case GO_TO_DECLARATION:
                GoToSupport.goTo(doc, offset, false);
                break;
            case ALT_HYPERLINK:
                JTextComponent focused = EditorRegistry.focusedComponent();
                
                if (focused != null && focused.getDocument() == doc) {
                    focused.setCaretPosition(offset);
                    GoToImplementation.goToImplementation(focused);
                }
                break;
        }
    }

    public String getTooltipText(Document doc, int offset, HyperlinkType type) {
        return GoToSupport.getGoToElementTooltip(doc, offset, false, type);
    }

}
