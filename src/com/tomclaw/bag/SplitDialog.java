package com.tomclaw.bag;

import com.alee.extended.filechooser.WebDirectoryChooser;
import com.alee.laf.filechooser.WebFileChooser;
import com.alee.utils.swing.DialogOptions;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.swing.SwingUtilities.invokeLater;

public class SplitDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField bagField;
    private JButton chooseBagButton;
    private JTextField dirField;
    private JButton chooseDirButton;
    private JTextField splitSizeField;

    public SplitDialog() {
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

        chooseBagButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSelectBag();
            }
        });

        chooseDirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSelectDir();
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

    private void onOK() {
        dispose();
        final ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.setSize(480, 120);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setVisible(true);
        final String bagFilePath = bagField.getText();
        final String directoryPath = dirField.getText();
        final Long splitSize = Long.parseLong(splitSizeField.getText());
        new Thread() {
            @Override
            public void run() {
                final File source = new File(bagFilePath);
                final String base = FilesHelper.getFileBaseFromName(source.getName());
                final List<Bag> bags = new ArrayList<>();
                try {
                    File dir = new File(directoryPath);
                    dir.mkdirs();
                    Node node = Node.scan(source);
                    final AtomicInteger files = new AtomicInteger(0);
                    node.walk(new Node.WalkCallback() {
                        @Override
                        public void onNode(String path, Node node) {
                            files.incrementAndGet();
                        }

                        @Override
                        public void onPath(String path) {
                        }
                    });
                    node.walk(new Node.WalkCallback() {

                        private Bag bag = null;
                        private long total = 0;
                        private DataOutputStream output;
                        private int fileNo = 0;

                        private Bag nextBag() throws IOException {
                            if (output != null) {
                                output.flush();
                                output.close();
                            }
                            if (bag != null) {
                                bags.add(bag);
                            }
                            String bagFileName = base + "_vol" + (bags.size() + 1) + ".bag";
                            File bagFile = new File(directoryPath, bagFileName);
                            bag = new Bag(bagFile.getAbsolutePath());
                            return bag;
                        }

                        @Override
                        public void onNode(String path, Node node) {
                            try {
                                if (bag == null || total >= splitSize) {
                                    bag = nextBag();
                                    total = 0;
                                    output = new DataOutputStream(new FileOutputStream(bag.getBagFile()));
                                }
                                path = path.substring(1 + base.length());
                                bag.write(output, new File(path, node.getName()).getAbsolutePath(), node.getLength(), node.getInputStream());
                                total += node.getLength();
                                fileNo++;
                                invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.setProgress(100 * fileNo / files.get());
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onPath(String path) {
                        }
                    });
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dispose();
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
        if (bagChooser.showOpenDialog(getOwner()) == WebFileChooser.APPROVE_OPTION) {
            File file = bagChooser.getSelectedFile();
            bagField.setText(file.getAbsolutePath());
        }
    }

    private void onSelectDir() {
        WebDirectoryChooser directoryChooser = new WebDirectoryChooser(getOwner(), "Choose any directory");
        directoryChooser.setVisible(true);

        if (directoryChooser.getResult() == DialogOptions.OK_OPTION) {
            File file = directoryChooser.getSelectedDirectory();
            dirField.setText(file.getAbsolutePath());
        }
    }
}
