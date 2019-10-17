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
package org.netbeans.modules.lsp.client.options;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import org.netbeans.modules.lsp.client.options.LanguageStorage.LanguageDescription;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle.Messages;

final class LanguageServersPanel extends javax.swing.JPanel {

    private static final LanguageDescription PROTOTYPE = new LanguageDescription(null, null, null, null, null, "MMMMMMMMMMMMMMMMM");
    private final LanguageServersOptionsPanelController controller;
    private final DefaultListModel<LanguageDescription> languages;
    private final Set<String> usedIds;

    LanguageServersPanel(LanguageServersOptionsPanelController controller) {
        this.controller = controller;
        this.languages = new DefaultListModel<>();
        this.usedIds = new HashSet<>();
        initComponents();
        this.languagesList.addListSelectionListener(evt -> setEnableDisable());
        setEnableDisable();
    }

    private void setEnableDisable() {
        edit.setEnabled(languagesList.getSelectedIndex() != (-1));
        remove.setEnabled(languagesList.getSelectedIndex() != (-1));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        languagesList = new javax.swing.JList<>();
        add = new javax.swing.JButton();
        edit = new javax.swing.JButton();
        remove = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(LanguageServersPanel.class, "LanguageServersPanel.jLabel1.text")); // NOI18N

        languagesList.setModel(languages);
        languagesList.setCellRenderer(new ListRenderer());
        languagesList.setPrototypeCellValue(PROTOTYPE);
        jScrollPane1.setViewportView(languagesList);

        org.openide.awt.Mnemonics.setLocalizedText(add, org.openide.util.NbBundle.getMessage(LanguageServersPanel.class, "LanguageServersPanel.add.text")); // NOI18N
        add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(edit, org.openide.util.NbBundle.getMessage(LanguageServersPanel.class, "LanguageServersPanel.edit.text")); // NOI18N
        edit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(remove, org.openide.util.NbBundle.getMessage(LanguageServersPanel.class, "LanguageServersPanel.remove.text")); // NOI18N
        remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 107, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(edit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(add, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(remove, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(add)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(edit)
                        .addGap(12, 12, 12)
                        .addComponent(remove)
                        .addGap(0, 11, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    @Messages("CAP_AddDescription=Add Language Description")
    private void addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addActionPerformed
        LanguageDescription desc = openCustomizeDialog(null, Bundle.CAP_AddDescription());
        if (desc != null) {
            languages.addElement(desc);
            controller.changed();
        }
    }//GEN-LAST:event_addActionPerformed

    private void editActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editActionPerformed
        LanguageDescription desc = openCustomizeDialog(languagesList.getSelectedValue(), Bundle.CAP_AddDescription());
        if (desc != null) {
            int pos = languagesList.getSelectedIndex();
            languages.remove(pos);
            languages.add(pos, desc);
            controller.changed();
        }
    }//GEN-LAST:event_editActionPerformed

    private void removeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeActionPerformed
        languages.remove(languagesList.getSelectedIndex());
        controller.changed();
    }//GEN-LAST:event_removeActionPerformed

    private LanguageDescription openCustomizeDialog(LanguageDescription desc, String title) {
        LanguageDescriptionPanel panel = new LanguageDescriptionPanel(desc, usedIds);
        DialogDescriptor dd = new DialogDescriptor(panel, title, true, DialogDescriptor.OK_CANCEL_OPTION, DialogDescriptor.OK_OPTION, null);
        if (DialogDisplayer.getDefault().notify(dd) == DialogDescriptor.OK_OPTION) {
            return panel.getDescription();
        }
        return null;
    }

    void load() {
        languages.clear();
        usedIds.clear();
        LanguageStorage.load().stream().forEach(desc -> {
            languages.addElement(desc);
            usedIds.add(desc.id);
        });
    }

    void store() {
        List<LanguageDescription> descriptions = new ArrayList<>();
        for (int i = 0; i < languages.size(); i++) {
            descriptions.add(languages.elementAt(i));
        }
        LanguageStorage.store(descriptions);
    }

    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton add;
    private javax.swing.JButton edit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<LanguageDescription> languagesList;
    private javax.swing.JButton remove;
    // End of variables declaration//GEN-END:variables

    private static final class ListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof LanguageDescription) {
                value = ((LanguageDescription) value).name;
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}
