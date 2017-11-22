package com.tomclaw.bag;

import com.alee.extended.filechooser.WebDirectoryChooser;
import com.alee.laf.filechooser.WebFileChooser;
import com.alee.utils.swing.DialogOptions;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class MergeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dirField;
    private JButton dirSelectButton;
    private JTextField bagField;
    private JButton bagSelectButton;
    private JTextField extField;

    public MergeDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

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

        dirSelectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSelectDir();
            }
        });

        bagSelectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSelectBag();
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

    private void onSelectDir() {
        WebDirectoryChooser directoryChooser = new WebDirectoryChooser(getOwner(), "Choose any directory");
        directoryChooser.setVisible(true);

        if (directoryChooser.getResult() == DialogOptions.OK_OPTION) {
            File file = directoryChooser.getSelectedDirectory();
            dirField.setText(file.getAbsolutePath());
        }
    }

    private void onSelectBag() {
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
            bagField.setText(file.getAbsolutePath());
        }
    }

    private void onOK() {
        final String dirText = dirField.getText();
        final String extText = extField.getText();
        final String bagText = bagField.getText();

        dispose();

        final ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.setSize(480, 120);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setVisible(true);

        new Thread() {

            int index = 0;

            @Override
            public void run() {

                final List<String> exts = Arrays.asList(extText.split(" "));

                File dir = new File(dirText);
                final List<File> volumes = new ArrayList<>();
                volumes.addAll(Arrays.asList(dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        boolean extCriteria = exts.contains(FilesHelper.getFileExtension(name));
                        boolean volCriteria = getFileNameVolume(name) > 0;
                        return extCriteria && volCriteria;
                    }
                })));

                Collections.sort(volumes, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        int vol1Index = getFileNameVolume(o1.getName());
                        int vol2Index = getFileNameVolume(o2.getName());
                        return Integer.compare(vol1Index, vol2Index);
                    }
                });

                int BUFFER_SIZE = 102400;
                byte[] buffer = new byte[BUFFER_SIZE];

                FileOutputStream output = null;
                try {
                    File bag = new File(bagText);
                    bag.getParentFile().mkdirs();
                    if (bag.exists()) {
                        bag.delete();
                    }
                    output = new FileOutputStream(bag);

                    FileInputStream input = null;

                    for (final File volume : volumes) {
                        try {
                            input = new FileInputStream(volume);
                            int read;
                            while ((read = input.read(buffer)) != -1) {
                                output.write(buffer, 0, read);
                            }
                        } finally {
                            if (input != null) {
                                try {
                                    input.close();
                                } catch (IOException ignored) {
                                }
                            }
                        }
                        index++;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.setProgress(100 * index / volumes.size());
                            }
                        });
                    }
                    output.flush();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dispose();
                        }
                    });
                } catch (Throwable ex) {
                    ex.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }.start();
    }

    private void onCancel() {
        dispose();
    }

    private static int getFileNameVolume(String fileName) {
        try {
            int volIndex = fileName.indexOf("_vol");
            if (volIndex > 0) {
                String baseName = FilesHelper.getFileBaseFromName(fileName);
                return Integer.parseInt(baseName.substring(volIndex + 4));
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }
}
