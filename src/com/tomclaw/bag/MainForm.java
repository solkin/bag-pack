package com.tomclaw.bag;

import com.alee.extended.layout.ToolbarLayout;
import com.alee.extended.list.FileListModel;
import com.alee.extended.list.FileListViewType;
import com.alee.extended.list.WebFileList;
import com.alee.extended.statusbar.WebMemoryBar;
import com.alee.extended.statusbar.WebStatusBar;
import com.alee.global.StyleConstants;
import com.alee.laf.button.WebButton;
import com.alee.laf.toolbar.ToolbarStyle;
import com.alee.laf.toolbar.WebToolBar;
import com.alee.laf.tree.WebTreeCellRenderer;
import com.alee.utils.file.FileComparator;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by solkin on 15/05/16.
 * Bag packer main frame.
 */
public class MainForm {
    private JPanel panel1;
    private JTree tree1;
    private JScrollPane contentScrollPane;
    private JPanel statusBarHolder;
    private JPanel toolbarHolder;
    private WebFileList webFileList;

    private static final boolean showFilesInTree = false;

    static MainForm instance;
    private JFrame frame;
    // private Bag bag;
    private Node tree;
    private Node selectedNode;

    public static void main(String[] args) {
        MainForm mainForm = new MainForm();
        JFrame frame = new JFrame("MainForm");
        mainForm.frame = frame;
        frame.setContentPane(mainForm.panel1);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        MainForm.instance = mainForm;
        mainForm.initUi();
    }

    private void initUi() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("<empty>", true);
        TreeModel treeModel = new DefaultTreeModel(root);

        Icon leafIcon = loadIcon("opened_folder.png");
        Icon openIcon = loadIcon("opened_folder.png");
        Icon closedIcon = loadIcon("closed_folder.png");
        Icon rootIcon = loadIcon("root_folder.png");
        WebTreeCellRenderer renderer = (WebTreeCellRenderer) tree1.getCellRenderer();
        renderer.setLeafIcon(leafIcon);
        renderer.setClosedIcon(closedIcon);
        renderer.setOpenIcon(openIcon);
        renderer.setRootIcon(rootIcon);

        tree1.setModel(treeModel);
        tree1.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            @SuppressWarnings("unchecked")
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
        webFileList.setGenerateThumbnails(false);
        webFileList.setPreferredColumnCount(4);
        webFileList.setPreferredRowCount(5);

        webFileList.addMouseListener(new MouseAdapter() {
            @Override
            @SuppressWarnings("unchecked")
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

        WebStatusBar statusBar = new WebStatusBar ();

        // Simple memory bar
        WebMemoryBar memoryBar = new WebMemoryBar ();
        memoryBar.setPreferredWidth ( memoryBar.getPreferredSize ().width + 20 );
        statusBar.add ( memoryBar, ToolbarLayout.END );

        statusBarHolder.add(statusBar);

        WebToolBar ut = new WebToolBar ( WebToolBar.HORIZONTAL );
        ut.setFloatable ( false );
        ut.setToolbarStyle(ToolbarStyle.attached);
        setupToolBar ( ut );
        toolbarHolder.add(ut);
    }

    private void setupToolBar(WebToolBar toolbar) {
        WebButton createButton = WebButton.createIconWebButton ( loadIcon ( "create.png" ), StyleConstants.smallRound, true );
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPackPressed();
            }
        });
        WebButton scanButton = WebButton.createIconWebButton ( loadIcon ( "scan.png" ), StyleConstants.smallRound, true );
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onScanPressed();
            }
        });
        WebButton unpackButton = WebButton.createIconWebButton ( loadIcon ( "unpack.png" ), StyleConstants.smallRound, true );
        unpackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onUnpackPressed();
            }
        });
        toolbar.add ( createButton );
        toolbar.add ( scanButton );
        toolbar.add ( unpackButton );
        toolbar.addSeparator ();
        toolbar.add ( WebButton.createIconWebButton ( loadIcon ( "append.png" ), StyleConstants.smallRound, true ) );
        toolbar.add ( WebButton.createIconWebButton ( loadIcon ( "delete.png" ), StyleConstants.smallRound, true ) );
    }

    private void onPackPressed() {
        PackDialog dialog = new PackDialog();
        dialog.pack();
        dialog.setSize(380, dialog.getHeight());
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void onScanPressed() {
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

    private void onUnpackPressed() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose directory to unpack");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
//                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                    File selectedFile = chooser.getSelectedFile();
//                    final File directory = new File(selectedFile.getAbsolutePath(), FilesHelper.getFileBaseFromName(bag.getName()));
//                    directory.mkdirs();
//                    final ProgressDialog progressDialog = new ProgressDialog();
//                    progressDialog.setSize(480, 120);
//                    progressDialog.setLocationRelativeTo(frame);
//                    progressDialog.setVisible(true);
//                    new Thread() {
//                        @Override
//                        public void run() {
//                            try {
//                                bag.unpack(directory.getAbsolutePath(), new Bag.BagProgressCallback() {
//                                    @Override
//                                    public void onProgress(final int percent) {
//                                        SwingUtilities.invokeLater(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                progressDialog.setProgress(percent);
//                                            }
//                                        });
//                                    }
//                                });
//                                SwingUtilities.invokeLater(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        progressDialog.dispose();
//                                    }
//                                });
//                            } catch (IOException ex) {
//                                ex.printStackTrace();
//                            }
//                        }
//                    }.start();
//                }
    }

    private ImageIcon loadIcon(String s) {
        return new ImageIcon(getClass().getResource("/res/" + s));
    }

    void openBag(final String path) {
        final File file = new File(path);
        new Thread() {
            @Override
            public void run() {
                try {
                    tree = Node.scan(file);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setTree(tree);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @SuppressWarnings("unchecked")
    private void setTree(Node tree) {
        this.tree = tree;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(tree.getName(), true);
        TreeModel treeModel = new DefaultTreeModel(root);

        walk(tree, root);

        selectedNode = tree;
        FileListModel tableModel = list(tree);

        webFileList.setModel(tableModel);
        tree1.setModel(treeModel);
    }

    private void walk(Node tree, DefaultMutableTreeNode root) {
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

    private FileListModel list(Node node) {
        File[] files = new File[node.list().size()];

        int c = 0;
        for (Node item : node.list()) {
            files[c++] = new VirtualFile(item.getName(), item.getLength());
        }

        Arrays.sort(files, new FileComparator());
        return new FileListModel(files);
    }
}
