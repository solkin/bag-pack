package com.tomclaw.bag;

import com.alee.laf.filechooser.WebFileChooser;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;

import static com.tomclaw.bag.StreamUtils.safeClose;

public class TarDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField bagField;
    private JTextField tarField;
    private JButton bagSelectButton;
    private JButton tarSelectButton;

    public TarDialog() {
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

        bagSelectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onBagSelect();
            }
        });

        tarSelectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onTarSelect();
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

    private void onBagSelect() {
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

    private void onTarSelect() {
        WebFileChooser bagChooser = new WebFileChooser();
        bagChooser.setMultiSelectionEnabled(false);
        bagChooser.setAcceptAllFileFilterUsed(false);
        bagChooser.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return FilesHelper.getFileExtension(f.getName()).equals("tar");
            }

            @Override
            public String getDescription() {
                return "Tar (*.tar)";
            }
        });
        if (bagChooser.showSaveDialog(getOwner()) == WebFileChooser.APPROVE_OPTION) {
            File file = bagChooser.getSelectedFile();
            tarField.setText(file.getAbsolutePath());
        }
    }

    private void onOK() {
        dispose();
        final ProgressDialog progressDialog = new ProgressDialog();
        progressDialog.setSize(480, 120);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setVisible(true);
        final String bagFilePath = bagField.getText();
        final String tarFilePath = tarField.getText();
        new Thread() {

            private OutputStream output = null;
            private int index = 0;
            private int total = 0;
            private long sumSize = 0;

            @Override
            public void run() {
                final File source = new File(bagFilePath);
                final File destination = new File(tarFilePath);

                destination.getParentFile().mkdirs();
                if (destination.exists()) {
                    destination.delete();
                }
                try {
                    FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(
                            source.toPath(), FileOwnerAttributeView.class);
                    UserPrincipal owner = ownerAttributeView.getOwner();

                    final String ownerName = owner.getName();

                    output = new FileOutputStream(destination, false);
                    final Node rootNode = Node.scan(source);

                    rootNode.walk(new Node.WalkCallback() {
                        @Override
                        public void onNode(String path, Node node) {
                            total++;
                        }

                        @Override
                        public void onPath(String path) {
                        }
                    });

                    rootNode.walk(new Node.WalkCallback() {

                        @Override
                        public void onNode(String path, Node node) {
                            String noRootPath = path.substring(rootNode.getName().length() + 1);
                            File file = new File(noRootPath, node.getName());
                            InputStream input = null;
                            try {
                                input = node.getInputStream();
                                sumSize += writeTarFile(file.getAbsolutePath(), node.getLength(), ownerName, input, output);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                safeClose(input);
                            }
                            index++;
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.setProgress(100 * index / total);
                                }
                            });
                        }

                        @Override
                        public void onPath(String path) {
                            String noRootPath = path.substring(rootNode.getName().length() + 1, path.length());
                            if (noRootPath.length() > 1) {
                                try {
                                    sumSize += writeTarFile(noRootPath, 0, ownerName, null, output);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    writeTrail(sumSize, output);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dispose();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    safeClose(output);
                }
            }
        }.start();
    }

    public static void writeTarString(OutputStream output, String string) throws IOException {
        writeTarString(output, string, true);
    }

    public static void writeTarString(OutputStream output, String string, boolean zeroTrim) throws IOException {
        writeTarBytes(output, string.getBytes(), zeroTrim);
    }

    public static void writeTarBytes(OutputStream output, byte[] data, boolean zeroTrim) throws IOException {
        output.write(data);
        output.write(' ');
        if (zeroTrim) {
            output.write(0);
        }
    }

    public static long writeTarFile(String filePath, long fileSize, String owner, InputStream input, OutputStream output) throws IOException {
        int type = 0;
        String fileMode = "000644";
        if (input == null) {
            type = 5;
            fileMode = "000755";
        }
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        filePath = filePath.substring(1);
        byte[] pathBytes;
        String pathCutted = filePath;
        while ((pathBytes = pathCutted.getBytes()).length >= 100) {
            pathCutted = pathCutted.substring(pathCutted.indexOf('/') + 1);
        }
        int pathShorten = filePath.length() - pathCutted.length();
        System.out.println("*> " + pathCutted);
        byte[] tarFilePath = new byte[100];
        System.arraycopy(pathBytes, 0, tarFilePath, 0, pathBytes.length);
        // Path.
        header.write(tarFilePath);
        // File mode.
        String mode = fileMode;
        writeTarString(header, mode);
        // Owner's numeric user ID.
        String userId = "000000";
        writeTarString(header, userId);
        // Group's numeric user ID.
        String groupId = "000000";
        writeTarString(header, groupId);
        // File size in bytes (octal base).
        byte[] octalSizeBytes = Long.toOctalString(fileSize).getBytes();
        byte[] fileSizeBytes = "00000000000".getBytes();
        System.arraycopy(octalSizeBytes, 0, fileSizeBytes, fileSizeBytes.length - octalSizeBytes.length, octalSizeBytes.length);
        writeTarBytes(header, fileSizeBytes, false);
        // Last modification time in numeric Unix time format (octal).
        long time = System.currentTimeMillis() / 1000;
        byte[] octalTimeBytes = Long.toOctalString(time).getBytes();
        byte[] modificationTimeBytes = "00000000000".getBytes();
        System.arraycopy(octalTimeBytes, 0, modificationTimeBytes, modificationTimeBytes.length - octalTimeBytes.length, octalTimeBytes.length);
        writeTarBytes(header, modificationTimeBytes, false);
        // Checksum for header record.
        int checksumOffset = header.size();
        byte[] checksumBytes = "        ".getBytes();
        header.write(checksumBytes);
        // Link indicator (file type).
        byte[] linkIndicatorBytes = String.valueOf(type).getBytes();
        header.write(linkIndicatorBytes);
        // Name of linked file.
        byte[] linkedFile = new byte[100];
        header.write(linkedFile);
        // Magic.
        String magic = "ustar";
        header.write(magic.getBytes());
        header.write(0);
        // Version.
        String version = "00";
        header.write(version.getBytes());
        // User name.
        byte[] ownerNameBytes = owner.getBytes();
        byte[] ownerNameTemplate = new byte[32];
        System.arraycopy(ownerNameBytes, 0, ownerNameTemplate, 0, ownerNameBytes.length);
        header.write(ownerNameTemplate);
        // Group name.
        byte[] groupNameBytes = "staff".getBytes();
        byte[] groupNameTemplate = new byte[32];
        System.arraycopy(groupNameBytes, 0, groupNameTemplate, 0, groupNameBytes.length);
        header.write(groupNameTemplate);
        // Dev Major.
        String devMajor = "000000";
        writeTarString(header, devMajor);
        // Dev Minor.
        String devMinor = "000000";
        writeTarString(header, devMinor);
        // Prefix.
        byte[] prefixBytes;
        if (pathShorten > 0) {
            String prePath = filePath.substring(0, pathShorten - 1);
            prefixBytes = prePath.getBytes();
        } else {
            prefixBytes = new byte[0];
        }
        byte[] prefixTemplate = new byte[155];
        System.arraycopy(prefixBytes, 0, prefixTemplate, 0, prefixBytes.length);
        header.write(prefixTemplate);
        // Block-align.
        byte[] something = new byte[12];
        header.write(something);
        // Header at all.
        byte[] headerBytes = header.toByteArray();
        int checksum = 0;
        for (byte headerByte : headerBytes) {
            checksum += headerByte;
        }
        // Checksum for header record.
        checksumBytes = Integer.toOctalString(checksum).getBytes();
        byte[] checksumTemplate = new byte[]{'0', '0', '0', '0', '0', '0', 0, ' '};
        System.arraycopy(checksumBytes, 0, checksumTemplate, checksumTemplate.length - 2 - checksumBytes.length, checksumBytes.length);
        System.arraycopy(checksumTemplate, 0, headerBytes, checksumOffset, checksumTemplate.length);
        // Write "checksummed" header.
        output.write(headerBytes);
        long sumSize = headerBytes.length;
        if (type == 0) {
            // File content.
            byte[] buffer = new byte[102400];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            sumSize += fileSize;
            // Postfix.
            int quantity = 512;
            if (sumSize % quantity != 0) {
                //long total = offset + sumSize;
                int blocksCount = (int) (sumSize / quantity);
                long totalSize = (blocksCount + 1) * quantity;
                int postfixSize = (int) (totalSize - sumSize);
                byte[] postfix = new byte[postfixSize];
                output.write(postfix);
                sumSize += postfixSize;
            }
        }
        output.flush();
        return sumSize;
    }

    public static void writeTrail(long sumSize, OutputStream output) throws IOException {
        // Postfix.
        int quantity = 512 * 20;
        if (sumSize % quantity != 0) {
            long blocksCount = (sumSize / quantity);
            long totalSize = (blocksCount + 1) * quantity;
            long trailSize = totalSize - sumSize;
            for (int c = 0; c < trailSize; c++) {
                output.write(0);
            }
        }
        output.flush();
    }

    private void onCancel() {
        dispose();
    }
}
