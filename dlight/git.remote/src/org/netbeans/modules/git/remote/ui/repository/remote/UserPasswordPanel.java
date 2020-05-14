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
package org.netbeans.modules.git.remote.ui.repository.remote;

/**
 *
 */
class UserPasswordPanel extends javax.swing.JPanel {

    /**
     * Creates new form UserPasswordPanel
     */
    public UserPasswordPanel () {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        passwordLabel.setLabelFor(userPasswordField);
        org.openide.awt.Mnemonics.setLocalizedText(passwordLabel, org.openide.util.NbBundle.getMessage(UserPasswordPanel.class, "UserPasswordPanel.passwordLabel.text")); // NOI18N
        passwordLabel.setToolTipText(org.openide.util.NbBundle.getMessage(UserPasswordPanel.class, "UserPasswordPanel.passwordLabel.toolTipText")); // NOI18N

        userLabel.setLabelFor(userTextField);
        org.openide.awt.Mnemonics.setLocalizedText(userLabel, org.openide.util.NbBundle.getMessage(UserPasswordPanel.class, "UserPasswordPanel.userLabel.text")); // NOI18N
        userLabel.setToolTipText(org.openide.util.NbBundle.getMessage(UserPasswordPanel.class, "UserPasswordPanel.userLabel.toolTipText")); // NOI18N

        userTextField.setColumns(8);
        userTextField.setMinimumSize(new java.awt.Dimension(11, 22));

        userPasswordField.setColumns(8);
        userPasswordField.setMinimumSize(new java.awt.Dimension(11, 22));

        savePasswordCheckBox.setMnemonic('v');
        org.openide.awt.Mnemonics.setLocalizedText(savePasswordCheckBox, org.openide.util.NbBundle.getMessage(UserPasswordPanel.class, "UserPasswordPanel.savePasswordCheckBox.text")); // NOI18N
        savePasswordCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(UserPasswordPanel.class, "UserPasswordPanel.savePasswordCheckBox.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(leaveBlankLabel, org.openide.util.NbBundle.getMessage(UserPasswordPanel.class, "UserPasswordPanel.leaveBlankLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(userLabel)
                    .addComponent(passwordLabel))
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(userPasswordField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(userTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(savePasswordCheckBox)
                    .addComponent(leaveBlankLabel)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(userLabel)
                    .addComponent(userTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(leaveBlankLabel))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(userPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(savePasswordCheckBox)))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(passwordLabel))))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JLabel leaveBlankLabel = new javax.swing.JLabel();
    final javax.swing.JLabel passwordLabel = new javax.swing.JLabel();
    final javax.swing.JCheckBox savePasswordCheckBox = new javax.swing.JCheckBox();
    final javax.swing.JLabel userLabel = new javax.swing.JLabel();
    final javax.swing.JPasswordField userPasswordField = new javax.swing.JPasswordField();
    final javax.swing.JTextField userTextField = new javax.swing.JTextField();
    // End of variables declaration//GEN-END:variables
}
