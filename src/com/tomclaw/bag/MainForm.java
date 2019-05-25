package com.tomclaw.bag;

import com.alee.extended.breadcrumb.WebBreadcrumb;
import com.alee.extended.breadcrumb.WebBreadcrumbButton;
import com.alee.extended.filechooser.WebDirectoryChooser;
import com.alee.extended.layout.ToolbarLayout;
import com.alee.extended.list.FileListModel;
import com.alee.extended.list.FileListViewType;
import com.alee.extended.list.WebFileList;
import com.alee.extended.statusbar.WebMemoryBar;
import com.alee.extended.statusbar.WebStatusBar;
import com.alee.global.StyleConstants;
import com.alee.laf.button.WebButton;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.toolbar.ToolbarStyle;
import com.alee.laf.toolbar.WebToolBar;
import com.alee.laf.tree.WebTreeCellRenderer;
import com.alee.managers.hotkey.Hotkey;
import com.alee.managers.style.skin.web.PopupStyle;
import com.alee.utils.file.FileComparator;
import com.alee.utils.swing.DialogOptions;

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
    private JPanel breadcumbHolder;
    private WebFileList webFileList;
    private WebBreadcrumb breadcrumb;

    private static final boolean showFilesInTree = false;

    static MainForm instance;
    private JFrame frame;
    // private Bag bag;
    private Node tree;
    private Node selectedNode;

    public static void main(String[] args) {
        MainForm mainForm = new MainForm();
        JFrame frame = new JFrame("Bag Pack");
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
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("/", true);
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
                if (node != null) updateSelectedNode(node);
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
                if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON3) {
                    int index = webFileList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        File selectedFile = webFileList.getSelectedFile();
                        final Node node = selectedNode.get(selectedFile.getName());

                        final WebPopupMenu popupMenu = new WebPopupMenu();
                        popupMenu.setPopupStyle(PopupStyle.simple);

                        WebMenuItem unpackItem = new WebMenuItem("Unpack", loadIcon("unpack.png"), Hotkey.ALT_U);
                        unpackItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                unpack(node);
                            }
                        });
                        WebMenuItem deleteItem = new WebMenuItem("Delete", loadIcon("delete.png"), Hotkey.DELETE);
                        deleteItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                delete(node);
                            }
                        });

                        popupMenu.add(unpackItem);
                        popupMenu.addSeparator();
                        popupMenu.add(deleteItem);
                        popupMenu.show(webFileList, e.getX(), e.getY());
                    }
                } else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int index = webFileList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        File selectedFile = webFileList.getSelectedFile();
                        if (selectedFile.isDirectory()) {
                            Node node = selectedNode.get(selectedFile.getName());
                            updateSelectedNode(node);
                        }
                    }
                }
            }
        });

        contentScrollPane.setViewportView(webFileList);

        WebStatusBar statusBar = new WebStatusBar();

        WebMemoryBar memoryBar = new WebMemoryBar();
        memoryBar.setPreferredWidth(memoryBar.getPreferredSize().width + 20);
        statusBar.add(memoryBar, ToolbarLayout.END);

        statusBarHolder.add(statusBar);

        WebToolBar ut = new WebToolBar(WebToolBar.HORIZONTAL);
        ut.setFloatable(false);
        ut.setToolbarStyle(ToolbarStyle.attached);
        setupToolBar(ut);
        toolbarHolder.add(ut);

        breadcrumb = new WebBreadcrumb(true);
        fillBreadcrumb();
        breadcumbHolder.add(breadcrumb);
    }

    private void fillBreadcrumb() {
        breadcrumb.removeAll();
        if (selectedNode != null) {
            Node node = selectedNode;
            do {
                final Node actionNode = node;
                WebBreadcrumbButton button = new WebBreadcrumbButton(node.getName());
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        updateSelectedNode(actionNode);
                    }
                });
                breadcrumb.add(button, 0);
            } while ((node = node.getParent()) != null);
        } else {
            breadcrumb.add(new WebBreadcrumbButton("/"));
        }
    }

    private void setupToolBar(WebToolBar toolbar) {
        WebButton createButton = WebButton.createIconWebButton(loadIcon("create.png"), StyleConstants.smallRound, true);
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPackPressed();
            }
        });
        WebButton scanButton = WebButton.createIconWebButton(loadIcon("scan.png"), StyleConstants.smallRound, true);
        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onScanPressed();
            }
        });
        WebButton unpackButton = WebButton.createIconWebButton(loadIcon("unpack.png"), StyleConstants.smallRound, true);
        unpackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onUnpackPressed();
            }
        });
        WebButton splitButton = WebButton.createIconWebButton(loadIcon("split.png"), StyleConstants.smallRound, true);
        splitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSplitPressed();
            }
        });
        WebButton mergeButton = WebButton.createIconWebButton(loadIcon("merge.png"), StyleConstants.smallRound, true);
        mergeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onMergePressed();
            }
        });
        toolbar.add(createButton);
        toolbar.add(scanButton);
        toolbar.add(unpackButton);
        toolbar.addSeparator();
        toolbar.add(splitButton);
        toolbar.add(mergeButton);
        toolbar.addSeparator();
        toolbar.add(WebButton.createIconWebButton(loadIcon("append.png"), StyleConstants.smallRound, true));
        toolbar.add(WebButton.createIconWebButton(loadIcon("delete.png"), StyleConstants.smallRound, true));
        WebButton tarButton = WebButton.createIconWebButton(loadIcon("save.png"), StyleConstants.smallRound, true);
        tarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTarPressed();
            }
        });
        toolbar.addSeparator();
        toolbar.add(tarButton);
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
        unpack(tree);
    }

    private void onSplitPressed() {
        SplitDialog dialog = new SplitDialog();
        dialog.pack();
        dialog.setSize(480, dialog.getHeight());
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void onMergePressed() {
        MergeDialog dialog = new MergeDialog();
        dialog.pack();
        dialog.setSize(480, dialog.getHeight());
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void onTarPressed() {
        TarDialog dialog = new TarDialog();
        dialog.pack();
        dialog.setSize(480, dialog.getHeight());
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
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

        tree1.setModel(treeModel);

        updateSelectedNode(tree);
    }

    private void updateSelectedNode(Node node) {
        selectedNode = node;

        fillBreadcrumb();

        FileListModel model = list(node);
        webFileList.setModel(model);
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

    private void unpack(final Node node) {
        WebDirectoryChooser directoryChooser = new WebDirectoryChooser(frame.getOwner(), "Choose directory to unpack");
        directoryChooser.setVisible(true);

        if (directoryChooser.getResult() == DialogOptions.OK_OPTION) {
            final File directory = directoryChooser.getSelectedDirectory();
            directory.mkdirs();
            final ProgressDialog progressDialog = new ProgressDialog();
            progressDialog.setSize(480, 120);
            progressDialog.setLocationRelativeTo(frame);
            progressDialog.setVisible(true);
            new Thread() {
                @Override
                public void run() {
                    final String unpackPath = directory.getAbsolutePath();
                    node.walk(new Node.WalkCallback() {
                        @Override
                        public void onNode(String path, Node node) {
                            File dir = new File(unpackPath, path);
                            dir.mkdirs();
                            try {
                                Node.saveFile(new File(dir, node.getName()), node.getInputStream());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
//                                SwingUtilities.invokeLater(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        progressDialog.setProgress(percent);
//                                    }
//                                });
                        }

                        @Override
                        public void onPath(String path) {
                        }
                    });
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dispose();
                        }
                    });
                }
            }.start();
        }
    }

    private void delete(final Node node) {
        new Thread() {
            @Override
            public void run() {
                try {
                    node.delete();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setTree(tree);
                            updateSelectedNode(tree);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
