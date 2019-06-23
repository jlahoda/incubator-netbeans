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
package org.netbeans.modules.java.duplicates;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.fold.FoldUtilities;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.java.duplicates.ComputeDuplicates.DuplicateDescription;
import org.netbeans.modules.java.duplicates.ComputeDuplicates.Span;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.ZOrder;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class DuplicatesListPanel extends javax.swing.JPanel {
    private final Collection<String> sourceRoots;
    private final Iterator<? extends DuplicateDescription> dupes;

    private int targetCount;

    public DuplicatesListPanel(Collection<String> sourceRoots, final Iterator<? extends DuplicateDescription> dupes) {
        this.sourceRoots = sourceRoots;
        this.dupes = dupes;
        
        initComponents();

        left.putClientProperty(DuplicatesListPanel.class, new OffsetsBag(left.getDocument()));
        left.setContentType("text/x-java");
        
        FoldHierarchy leftFolds = FoldHierarchy.get(left);
        leftFolds.addFoldHierarchyListener(evt -> {
            System.err.println("foldEvent left");
            FoldUtilities.expandAll(leftFolds);
            scroll(left);
        });

        right.putClientProperty(DuplicatesListPanel.class, new OffsetsBag(right.getDocument()));
        right.setContentType("text/x-java");
        
        FoldHierarchy rightFolds = FoldHierarchy.get(right);
        rightFolds.addFoldHierarchyListener(evt -> {
            System.err.println("foldEvent right");
            FoldUtilities.expandAll(rightFolds);
            scroll(right);
        });

        duplicatesList.setModel(new DefaultListModel());
        duplicatesList.setCellRenderer(new DuplicatesRendererImpl());
        duplicatesList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent arg0) {
                DuplicateDescription dd = (DuplicateDescription) duplicatesList.getSelectedValue();
                DefaultComboBoxModel l = new DefaultComboBoxModel();
                DefaultComboBoxModel r = new DefaultComboBoxModel();

                for (Span s : dd.dupes) {
                    l.addElement(s);
                    r.addElement(s);
                }

                leftFileList.setModel(l);
                rightFileList.setModel(r);

                leftFileList.setSelectedIndex(0);
                rightFileList.setSelectedIndex(1);
            }
        });

        leftFileList.setRenderer(new SpanRendererImpl());
        leftFileList.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setSpan(left, (Span) leftFileList.getSelectedItem());
            }
        });
        rightFileList.setRenderer(new SpanRendererImpl());
        rightFileList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSpan(right, (Span) rightFileList.getSelectedItem());
            }
        });

        progressLabel.setText("Looking for duplicates...");

        findMore();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jScrollPane1 = new javax.swing.JScrollPane();
        duplicatesList = new javax.swing.JList();
        mainSplit2 = new BalancedSplitPane();
        rightPanel = new javax.swing.JPanel();
        rightFileList = new javax.swing.JComboBox();
        jScrollPane3 = new javax.swing.JScrollPane();
        right = new javax.swing.JEditorPane();
        leftPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        left = new javax.swing.JEditorPane();
        leftFileList = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        findMore = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        duplicatesList.setPrototypeCellValue("9999999999999999999999999999999999999999999999999999999999999999999999");
        duplicatesList.setVisibleRowCount(4);
        jScrollPane1.setViewportView(duplicatesList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 884;
        gridBagConstraints.ipady = 45;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
        add(jScrollPane1, gridBagConstraints);

        mainSplit2.setDividerLocation(400);

        rightPanel.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 324;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        rightPanel.add(rightFileList, gridBagConstraints);

        jScrollPane3.setViewportView(right);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        rightPanel.add(jScrollPane3, gridBagConstraints);

        mainSplit2.setRightComponent(rightPanel);

        leftPanel.setLayout(new java.awt.GridBagLayout());

        jScrollPane2.setViewportView(left);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        leftPanel.add(jScrollPane2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        leftPanel.add(leftFileList, gridBagConstraints);

        mainSplit2.setLeftComponent(leftPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 506;
        gridBagConstraints.ipady = 413;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 12);
        add(mainSplit2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        progressLabel.setText(org.openide.util.NbBundle.getMessage(DuplicatesListPanel.class, "DuplicatesListPanel.progressLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(progressLabel, gridBagConstraints);

        findMore.setText(org.openide.util.NbBundle.getMessage(DuplicatesListPanel.class, "DuplicatesListPanel.findMore.text")); // NOI18N
        findMore.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        findMore.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                findMoreMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(findMore, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 348, 12, 12);
        add(jPanel1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void findMoreMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_findMoreMouseClicked
        findMore();
    }//GEN-LAST:event_findMoreMouseClicked

    private void findMore() {
        targetCount = duplicatesList.getModel().getSize() + 100;
        findMore.setVisible(false);
        WORKER.schedule(0);
    }

    private static String computeCommonPrefix(String origCommonPrefix, FileObject file) {
        String name = FileUtil.getFileDisplayName(file);

        if (origCommonPrefix == null) return name;

        int len = Math.min(origCommonPrefix.length(), name.length());

        for (int cntr = 0; cntr < len; cntr++) {
            if (origCommonPrefix.charAt(cntr) != name.charAt(cntr)) {
                return origCommonPrefix.substring(0, cntr);
            }
        }

        return origCommonPrefix;
    }
    
    private static void setSpan(JEditorPane pane, Span s) {
        try {
            pane.putClientProperty(Span.class, null);
            pane.setText(s.file.asText());
            pane.putClientProperty(Span.class, s);
            
            scroll(pane);

            OffsetsBag bag = (OffsetsBag) pane.getClientProperty(DuplicatesListPanel.class);

            bag.clear();
            bag.addHighlight(s.startOff, s.endOff, HIGHLIGHT);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
//        } catch (BadLocationException ex) {
//            Exceptions.printStackTrace(ex);
        }
    }
    
    private static void scroll(JEditorPane pane) {
        Span s = (Span) pane.getClientProperty(Span.class);

        if (s == null) {
            return ;
        }

        try {
//            Rectangle top = pane.modelToView(0);
            Rectangle start = pane.modelToView(s.startOff);
//            Rectangle end = pane.modelToView(s.endOff);

            if (start != null) {
//                Rectangle toScroll = start.union(end);

                SwingUtilities.invokeLater(() -> {
                    Rectangle viewport = pane.getVisibleRect(); 
                    int offset = viewport.height / 3;
                    start.height = viewport.height  - offset;
                    start.y -= offset;
                    pane.scrollRectToVisible(start);
                    try {
                        pane.setCaretPosition(Utilities.getRowStart(pane, s.startOff));
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static final AttributeSet HIGHLIGHT = AttributesUtilities.createImmutable(StyleConstants.Background, new Color(0xDF, 0xDF, 0xDF, 0xff));

    private final class DuplicatesRendererImpl extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (!(value instanceof DuplicateDescription)) return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            DuplicateDescription dd = (DuplicateDescription) value;
            Set<FileObject> files = new LinkedHashSet<FileObject>();
            String commonPrefix = null;

            for (Span s : dd.dupes) {
                commonPrefix = computeCommonPrefix(commonPrefix, s.file);
                files.add(s.file);
            }

            StringBuilder cap = new StringBuilder();

            OUTER: for (FileObject file : files) {
                String name = FileUtil.getFileDisplayName(file);

                if (cap.length() > 0) {
                    cap.append("    ");
                }
                
                for (String sr : sourceRoots) {
                    if (name.startsWith(sr)) {
                        cap.append(name.substring(Math.max(0, sr.lastIndexOf('/') + 1)));
                        continue OUTER;
                    }
                }
            }

            return super.getListCellRendererComponent(list, cap.toString(), index, isSelected, cellHasFocus);
        }
    }

    private final class SpanRendererImpl extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (!(value instanceof Span)) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
            Span span = (Span) value;

            return super.getListCellRendererComponent(list, FileUtil.getFileDisplayName(span.file), index, isSelected, cellHasFocus);
        }
    }

    @MimeRegistration(mimeType="text/x-java", service=HighlightsLayerFactory.class)
    public static final class HighlightLayerFactoryImpl implements HighlightsLayerFactory {
        public HighlightsLayer[] createLayers(Context cntxt) {
            OffsetsBag bag = (OffsetsBag) cntxt.getComponent().getClientProperty(DuplicatesListPanel.class);

            if (bag != null) {
                return new HighlightsLayer[] {
                    HighlightsLayer.create(DuplicatesListPanel.class.getName(), ZOrder.CARET_RACK, true, bag)
                };
            }

            return new HighlightsLayer[0];
        }
    }

//    @ServiceProvider(service=MimeDataProvider.class)
//    public static final class MDPI implements MimeDataProvider {
//
//        private static final Lookup L = Lookups.singleton(new HighlightLayerFactoryImpl());
//
//        public Lookup getLookup(MimePath mp) {
//            if (mp.getPath().startsWith("text/x-java")) {
//                return L;
//            }
//
//            return null;
//        }
//        
//    }

    private static final class BalancedSplitPane extends JSplitPane {

        @Override
        @SuppressWarnings("deprecation")
        public void reshape(int x, int y, int w, int h) {
            System.err.println("reshape, width: " + w);
            super.reshape(x, y, w, h);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setDividerLocation(0.5);
                }
            });
        }

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList duplicatesList;
    private javax.swing.JLabel findMore;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JEditorPane left;
    private javax.swing.JComboBox leftFileList;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JSplitPane mainSplit2;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JEditorPane right;
    private javax.swing.JComboBox rightFileList;
    private javax.swing.JPanel rightPanel;
    // End of variables declaration//GEN-END:variables

    private static final RequestProcessor DEFAULT_WORKER = new RequestProcessor(DuplicatesListPanel.class.getName(), 1, false, false);
    private final Task WORKER = DEFAULT_WORKER.create(new Runnable() {
        public void run() {
            if (dupes.hasNext()) {
                final DuplicateDescription dd = dupes.next();

                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        ((DefaultListModel)duplicatesList.getModel()).addElement(dd);

                        int size = duplicatesList.getModel().getSize();

                        if (size == 1) {
                            duplicatesList.setSelectedIndex(0);
                        }
                        
                        if (size >= targetCount) {
                            findMore.setVisible(true);
                            progressLabel.setText("Found " + size + " duplicated snippets.");
                        } else {
                            progressLabel.setText("Found " + size + " duplicated snippets and searching...");
                            WORKER.schedule(0);
                        }
                    }
                });
            }
        }
    });

}
