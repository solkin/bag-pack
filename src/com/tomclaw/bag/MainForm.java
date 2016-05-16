package com.tomclaw.bag;

import com.alee.extended.list.FileElement;
import com.alee.extended.list.FileListModel;
import com.alee.extended.list.FileListViewType;
import com.alee.extended.list.WebFileList;
import com.alee.laf.tree.WebTreeCellRenderer;
import com.alee.utils.FileUtils;
import com.alee.utils.file.FileComparator;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by solkin on 15/05/16.
 */
public class MainForm {
    private JPanel panel1;
    private JTree tree1;
    private JButton packButton;
    private JButton scanButton;
    private JButton unpackButton;
    private JButton appendButton;
    private JButton deleteButton;
    private JScrollPane contentScrollPane;
    private WebFileList webFileList;

    private static boolean showFilesInTree = false;

    public static MainForm instance;
    private JFrame frame;
    private Bag bag;
    private Node tree;
    private Node selectedNode;

    public static void main(String[] args) {
        MainForm mainForm = new MainForm();
        JFrame frame = new JFrame("MainForm");
        mainForm.frame = frame;
        frame.setContentPane(mainForm.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        MainForm.instance = mainForm;
        mainForm.initUi();
    }

    private void initUi() {
        packButton.setBorderPainted(false);
        scanButton.setBorderPainted(false);
        unpackButton.setBorderPainted(false);
        appendButton.setBorderPainted(false);
        deleteButton.setBorderPainted(false);

        packButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PackDialog dialog = new PackDialog();
                dialog.pack();
                dialog.setSize(380, dialog.getHeight());
                dialog.setLocationRelativeTo(frame);
                dialog.setVisible(true);
            }
        });

        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Choose bag");
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return FilesHelper.getFileExtension(f.getName()).equals("bag");
                    }

                    @Override
                    public String getDescription() {
                        return "Bags (*.bag)";
                    }
                });
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    openBag(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        unpackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Choose directory to unpack");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = chooser.getSelectedFile();
                    File directory = new File(selectedFile.getAbsolutePath(), FilesHelper.getFileBaseFromName(bag.getName()));
                    directory.mkdirs();
                    ProgressDialog progressDialog = new ProgressDialog();
                    progressDialog.setSize(480, 120);
                    progressDialog.setLocationRelativeTo(frame);
                    progressDialog.setVisible(true);
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                bag.unpack(directory.getAbsolutePath(), new Bag.BagProgressCallback() {
                                    @Override
                                    public void onProgress(int percent) {
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
                                    }
                                });
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        });

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("<empty>", true);
        TreeModel treeModel = new DefaultTreeModel(root);

        Icon leafIcon = new ImageIcon(getClass().getResource("/res/opened_folder.png"));
        Icon openIcon = new ImageIcon(getClass().getResource("/res/opened_folder.png"));
        Icon closedIcon = new ImageIcon(getClass().getResource("/res/closed_folder.png"));
        Icon rootIcon = new ImageIcon(getClass().getResource("/res/root_folder.png"));
        WebTreeCellRenderer renderer = (WebTreeCellRenderer) tree1.getCellRenderer();
        renderer.setLeafIcon(leafIcon);
        renderer.setClosedIcon(closedIcon);
        renderer.setOpenIcon(openIcon);
        renderer.setRootIcon(rootIcon);

        tree1.setModel(treeModel);
        tree1.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath treePath = e.getPath();
                Node node = tree;
                for (int c = 1; c < treePath.getPathCount(); c++) {
                    String name = e.getPath().getPathComponent(c).toString();
                    node = node.get(name);
                }
                selectedNode = node;
                FileListModel model = list(node);
                webFileList.setModel(model);
            }
        });

        webFileList = new WebFileList();
        webFileList.setFileListViewType(FileListViewType.tiles);
        webFileList.setGenerateThumbnails(true);
        webFileList.setPreferredColumnCount(4);
        webFileList.setPreferredRowCount(5);

        webFileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = webFileList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        File selectedFile = webFileList.getSelectedFile();
                        if (selectedFile.isDirectory()) {
                            Node node = selectedNode.get(selectedFile.getName());
                            selectedNode = node;

                            FileListModel model = list(node);
                            webFileList.setModel(model);
                        }
                    }
                }
            }
        });

        contentScrollPane.setViewportView(webFileList);
    }

    public void openBag(String path) {
        bag = new Bag(path);
        new Thread() {
            @Override
            public void run() {
                try {
                    Node tree = bag.scan();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showTree(FilesHelper.getFileName(path), tree);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void showTree(String title, Node tree) {
        this.tree = tree;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(title, true);
        TreeModel treeModel = new DefaultTreeModel(root);

        walk(tree, root);

        selectedNode = tree;
        FileListModel tableModel = list(tree);

        webFileList.setModel(tableModel);
        tree1.setModel(treeModel);
    }

    public void walk(Node tree, DefaultMutableTreeNode root) {
        for (Node node : tree.list()) {
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node.getName(), true);
            boolean isFile = node.size() == 0;
            if (showFilesInTree || !isFile) {
                root.add(treeNode);
            }
            if (node.size() > 0) {
                walk(node, treeNode);
            }
        }
    }

    public FileListModel list(Node node) {
        File[] files = new File[node.list().size()];

        int c = 0;
        for (Node item : node.list()) {
            files[c++] = new VirtualFile(item.getName(), item.getLength());
        }

        Arrays.sort(files, new FileComparator());
        FileListModel model = new FileListModel(files);
//        for (FileElement element : model.getElements()) {
//            // ImageIcon leafIcon = new ImageIcon(getClass().getResource("/res/opened_folder.png"));
//            // ImageIO.
//            // bag.unpack();
//            // element.setEnabledThumbnail(icon);
//        }
        return model;
    }
}
