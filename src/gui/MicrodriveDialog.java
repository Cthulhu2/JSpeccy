/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MicrodriveDialog.java
 *
 * Created on 20-jun-2011, 8:36:16
 */
package gui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import machine.Interface1;

/**
 *
 * @author jsanchez
 */
public class MicrodriveDialog extends javax.swing.JPanel {

    private JDialog microdriveDialog;
    private Interface1 if1;
    private MicrodriveTableModel tableModel;
    private JFileChooser cartridgeDlg;
    private File currentDir;
    ExtensionFilter mdrExtension = new ExtensionFilter("CARTRIDGE_TYPE", ".mdr");

    /** Creates new form MicrodriveDialog */
    public MicrodriveDialog(Interface1 handler) {
        if1 = handler;
        tableModel = new MicrodriveTableModel();
        initComponents();
        MouseListener popupListener = new PopupListener();
        microdrivesTable.addMouseListener(popupListener);
        microdrivesTable.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent evt) {
                int row = microdrivesTable.rowAtPoint(evt.getPoint());
                microdrivesTable.setToolTipText(if1.getAbsolutePath(row));
            }
        });
        
        microdrivesTable.getColumnModel().getColumn(0).setPreferredWidth(25);
        microdrivesTable.getColumnModel().getColumn(2).setPreferredWidth(25);
        microdrivesTable.getColumnModel().getColumn(3).setPreferredWidth(25);
    }

    public boolean showDialog(Component parent, String title) {
        Frame owner = null;
        if (parent instanceof Frame) {
            owner = (Frame) parent;
        } else {
            owner = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);
        }

        if (microdriveDialog == null || microdriveDialog.getOwner() != owner) {
            owner = null;
            microdriveDialog = new JDialog(owner, true);
            microdriveDialog.getContentPane().add(this);
            microdriveDialog.pack();
        }

        microdriveDialog.setTitle(title);
        microdriveDialog.setVisible(true);
        return true;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popupMenu = new javax.swing.JPopupMenu();
        driveNumber = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        newCartridge = new javax.swing.JMenuItem();
        openCartridge = new javax.swing.JMenuItem();
        ejectCartridge = new javax.swing.JMenuItem();
        saveCartridge = new javax.swing.JMenuItem();
        saveAsCartridge = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        microdrivesTable = new javax.swing.JTable();

        driveNumber.setText("jMenuItem1");
        driveNumber.setEnabled(false);
        popupMenu.add(driveNumber);
        popupMenu.add(jSeparator1);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("gui/Bundle"); // NOI18N
        newCartridge.setText(bundle.getString("MicrodriveDialog.popupMenu.newCartridge.text")); // NOI18N
        newCartridge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newCartridgeActionPerformed(evt);
            }
        });
        popupMenu.add(newCartridge);

        openCartridge.setText(bundle.getString("MicrodriveDialog.popupMenu.openCartridge.text")); // NOI18N
        openCartridge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openCartridgeActionPerformed(evt);
            }
        });
        popupMenu.add(openCartridge);

        ejectCartridge.setText(bundle.getString("MicrodriveDialog.popupMenu.ejectCartridge.text")); // NOI18N
        ejectCartridge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ejectCartridgeActionPerformed(evt);
            }
        });
        popupMenu.add(ejectCartridge);

        saveCartridge.setText(bundle.getString("MicrodriveDialog.popupMenu.saveCartridge.text")); // NOI18N
        saveCartridge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveCartridgeActionPerformed(evt);
            }
        });
        popupMenu.add(saveCartridge);

        saveAsCartridge.setText(bundle.getString("MicrodriveDialog.popupMenu.saveAsCartridge.text")); // NOI18N
        saveAsCartridge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsCartridgeActionPerformed(evt);
            }
        });
        popupMenu.add(saveAsCartridge);

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));

        closeButton.setText(bundle.getString("MicrodriveDialog.closeButton.text")); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        microdrivesTable.setModel(tableModel);
        microdrivesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        microdrivesTable.getTableHeader().setResizingAllowed(false);
        microdrivesTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(microdrivesTable);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(150, Short.MAX_VALUE)
                .addComponent(closeButton)
                .addGap(144, 144, 144))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(closeButton)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        add(jPanel1);
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        microdriveDialog.setVisible(false);
    }//GEN-LAST:event_closeButtonActionPerformed

    private void newCartridgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newCartridgeActionPerformed
        int row = microdrivesTable.getSelectedRow();
        if1.insertNew(row);
        tableModel.fireTableRowsUpdated(row, row);
        ResourceBundle bundle = ResourceBundle.getBundle("gui/Bundle"); // NOI18N
            int ret = JOptionPane.showConfirmDialog(microdriveDialog.getContentPane(),
                  bundle.getString("NEW_CARTRIDGE_WARNING"),
                  bundle.getString("NEW_CARTRIDGE_WARNING_TITLE"),
                  JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE); // NOI18N
    }//GEN-LAST:event_newCartridgeActionPerformed

    private void openCartridgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openCartridgeActionPerformed

        int row = microdrivesTable.getSelectedRow();
        
        if (cartridgeDlg == null) {
            cartridgeDlg = new JFileChooser("/home/jsanchez/Spectrum");
            cartridgeDlg.addChoosableFileFilter(mdrExtension);
            cartridgeDlg.setFileFilter(mdrExtension);
            currentDir = cartridgeDlg.getCurrentDirectory();
        } else {
            cartridgeDlg.setCurrentDirectory(currentDir);
        }
        
        int status = cartridgeDlg.showOpenDialog(this);
        if (status == JFileChooser.APPROVE_OPTION) {
            currentDir = cartridgeDlg.getCurrentDirectory();
//            File filename = new File(cartridgeDlg.getSelectedFile().getAbsolutePath());
                if1.insertFile(row, cartridgeDlg.getSelectedFile());
                tableModel.fireTableRowsUpdated(row, row);
        }
    }//GEN-LAST:event_openCartridgeActionPerformed

    private void ejectCartridgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ejectCartridgeActionPerformed

        int row = microdrivesTable.getSelectedRow();
        
        if (if1.isModified(row)) {
            ResourceBundle bundle = ResourceBundle.getBundle("gui/Bundle"); // NOI18N
            int ret = JOptionPane.showConfirmDialog(microdriveDialog.getContentPane(),
                  bundle.getString("EJECT_CARTRIDGE_MSG"), bundle.getString("EJECT_CARTRIDGE_TITLE"),
                  JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE); // NOI18N
            if( ret == JOptionPane.NO_OPTION ) {
                return;
            }
        }
        
        if1.eject(row);
        tableModel.fireTableRowsUpdated(row, row);
    }//GEN-LAST:event_ejectCartridgeActionPerformed

    private void saveCartridgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCartridgeActionPerformed

        int row = microdrivesTable.getSelectedRow();
        
        if1.save(row);
        tableModel.fireTableRowsUpdated(row, row);
    }//GEN-LAST:event_saveCartridgeActionPerformed

    private void saveAsCartridgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsCartridgeActionPerformed
        int row = microdrivesTable.getSelectedRow();
        
        if (cartridgeDlg == null) {
            cartridgeDlg = new JFileChooser("/home/jsanchez/Spectrum");
            cartridgeDlg.addChoosableFileFilter(mdrExtension);
            cartridgeDlg.setFileFilter(mdrExtension);
            currentDir = cartridgeDlg.getCurrentDirectory();
        } else {
            cartridgeDlg.setCurrentDirectory(currentDir);
        }
        
        int status = cartridgeDlg.showSaveDialog(this);
        if (status == JFileChooser.APPROVE_OPTION) {
            currentDir = cartridgeDlg.getCurrentDirectory();
//            File filename = new File(cartridgeDlg.getSelectedFile().getAbsolutePath());
                if1.save(row, cartridgeDlg.getSelectedFile());
                tableModel.fireTableRowsUpdated(row, row);
        }
    }//GEN-LAST:event_saveAsCartridgeActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JMenuItem driveNumber;
    private javax.swing.JMenuItem ejectCartridge;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTable microdrivesTable;
    private javax.swing.JMenuItem newCartridge;
    private javax.swing.JMenuItem openCartridge;
    private javax.swing.JPopupMenu popupMenu;
    private javax.swing.JMenuItem saveAsCartridge;
    private javax.swing.JMenuItem saveCartridge;
    // End of variables declaration//GEN-END:variables

    public class PopupListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent evt) {
            showPopup(evt);
        }

        @Override
        public void mouseReleased(MouseEvent evt) {
            showPopup(evt);
        }

        private void showPopup(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                if (microdrivesTable.getSelectedRowCount() == 0) {
                    driveNumber.setText("Microdrive");
                    newCartridge.setEnabled(false);
                    openCartridge.setEnabled(false);
                    ejectCartridge.setEnabled(false);
                    saveCartridge.setEnabled(false);
                    saveAsCartridge.setEnabled(false);
                } else {
                    int row = microdrivesTable.getSelectedRow();
                    driveNumber.setText(String.format("Microdrive %d", row + 1));
                    if (if1.isCartridge(row)) {
                        newCartridge.setEnabled(false);
                        openCartridge.setEnabled(false);
                        ejectCartridge.setEnabled(true);
                        saveCartridge.setEnabled(if1.getFilename(row) != null);
                        saveAsCartridge.setEnabled(true);
                    } else {
                        newCartridge.setEnabled(true);
                        openCartridge.setEnabled(true);
                        ejectCartridge.setEnabled(false);
                        saveCartridge.setEnabled(false);
                        saveAsCartridge.setEnabled(false);
                    }
                }
                popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }

    private class MicrodriveTableModel extends AbstractTableModel {

        public MicrodriveTableModel() {
        }

        @Override
        public int getRowCount() {
            return if1.getNumDrives();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 2;
        }

        @Override
        public Class getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        @Override
        public Object getValueAt(int row, int col) {

//        System.out.println(String.format("getValueAt row %d, col %d", row, col));
            switch (col) {
                case 0:
                    return String.format("%4d", row + 1);
                case 1:
                    java.util.ResourceBundle bundle =
                            java.util.ResourceBundle.getBundle("gui/Bundle"); // NOI18N

                    if (!if1.isCartridge(row)) {
                        return bundle.getString("MicrodriveDialog.tableModel.nocartridge.txt");
                    }

                    String name = if1.getFilename(row);
                    if (name == null) {
                        name = bundle.getString("MicrodriveDialog.tableModel.nofilename.txt");
                    }
                    return name;
                case 2:
                    return if1.isWriteProtected(row);
                case 3:
                    return if1.isModified(row);
                default:
                    return "NOT EXISTANT COLUMN!";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 2) {
                Boolean state = (Boolean) value;
                if1.setWriteProtected(row, state);
                fireTableRowsUpdated(row, row);
            }
        }

        @Override
        public String getColumnName(int col) {
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("gui/Bundle"); // NOI18N

            String msg;

            switch (col) {
                case 0:
                    msg = bundle.getString("MicrodriveDialog.tableModel.column0.txt");
                    break;
                case 1:
                    msg = bundle.getString("MicrodriveDialog.tableModel.column1.txt");
                    break;
                case 2:
                    msg = bundle.getString("MicrodriveDialog.tableModel.column2.txt");
                    break;
                case 3:
                    msg = bundle.getString("MicrodriveDialog.tableModel.column3.txt");
                    break;
                default:
                    msg = "COLUMN ERROR!";
            }
            return msg;
        }
    }
}
