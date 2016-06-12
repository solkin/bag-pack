package com.tomclaw.bag;

import com.alee.extended.filechooser.WebDirectoryChooser;
import com.alee.global.GlobalConstants;
import com.alee.laf.filechooser.WebFileChooser;
import com.alee.utils.swing.DialogOptions;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

public class PackDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField directoryField;
    private JTextField bagField;
    private JButton chooseDirectoryButton;
    private JButton chooseBagButton;
    private JCheckBox openAfterCreateCheckBox;
    private JTextField excludeField;

    public PackDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        chooseDirectoryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onChooseDirectory();
            }
        });

        chooseBagButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onChooseBag();
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onChooseDirectory() {
        WebDirectoryChooser directoryChooser = new WebDirectoryChooser(getOwner(), "Choose any directory");
        directoryChooser.setVisible(true);

        if (directoryChooser.getResult() == DialogOptions.OK_OPTION) {
            File file = directoryChooser.getSelectedDirectory();
            directoryField.setText(file.getAbsolutePath());
        }
    }

    private void onChooseBag() {
        WebFileChooser bagChooser = new WebFileChooser();
        bagChooser.setMultiSelectionEnabled(false);
        bagChooser.setAcceptAllFileFilterUsed(false);
        bagChooser.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return FilesHelper.getFileExtension(f.getName()).equals("bag");
            }

            @Override
            public String getDescription() {
                return "Bags (*.bag)";
            }
        });
        if (bagChooser.showSaveDialog(getOwner()) == WebFileChooser.APPROVE_OPTION) {
            File file = bagChooser.getSelectedFile();
            bagField.setText(file.getAbsolutePath() + ".bag");
        }
    }

    private void onOK() {
        dispose();
        final ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.setSize(480, 120);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setVisible(true);
        final String bagFilePath = bagField.getText();
        final String directoryPath = directoryField.getText();
        final String[] exclude;
        if (excludeField.getText().length() > 0) {
            exclude = excludeField.getText().split(",");
        } else {
            exclude = new String[0];
        }
        final boolean open = openAfterCreateCheckBox.isSelected();
        new Thread() {
            @Override
            public void run() {
                File directory = new File(directoryPath);
                List<File> files = FilesHelper.listFiles(directory, new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if (exclude.length != 0) {
                            for (String ext : exclude) {
                                if (FilesHelper.getFileExtension(name).equals(ext)) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                });
                Bag bag = new Bag(bagFilePath);
                try {
                    bag.pack(directoryPath, files, new Bag.BagProgressCallback() {
                        @Override
                        public void onProgress(final int percent) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.setProgress(percent);
                                }
                            });
                        }
                    });
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dispose();
                            if (open) {
                                MainForm.instance.openBag(bagFilePath);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) {
        PackDialog dialog = new PackDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
