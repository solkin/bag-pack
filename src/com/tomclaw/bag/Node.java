package com.tomclaw.bag;

import java.io.*;
import java.util.*;

import static com.tomclaw.bag.StreamUtils.safeClose;

/**
 * Created by solkin on 15/05/16.
 */
@SuppressWarnings("WeakerAccess")
public class Node extends HashMap<String, Node> {

    private static final int BUFFER_SIZE = 102400;
    private static final byte[] buffer = new byte[BUFFER_SIZE];
    private static final String PATH_SEPARATOR = "/";

    private Node parent;
    private String name;
    private long length;

    private File file;
    private long descriptor;

    public Node() {
        this.parent = null;
        this.name = PATH_SEPARATOR;
        this.length = -1;
        this.descriptor = -1;
        this.file = null;
    }

    public Node(Node parent, String name, long length, File file, long descriptor) {
        this.parent = parent;
        this.name = name;
        this.length = length;
        this.file = file;
        this.descriptor = descriptor;
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }

    public Node getParent() {
        return parent;
    }

    public File getFile() {
        return file;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDescriptor() {
        return descriptor;
    }

    private void moveDescriptor(long delta) {
        descriptor += delta;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void add(String path, long length, long descriptor) {
        String[] items = path.split(PATH_SEPARATOR);
        Node node = this;
        for (int c = 1; c < items.length; c++) {
            long itemLength = -1;
            long itemDescriptor = -1;
            if (c == items.length - 1) {
                itemLength = length;
                itemDescriptor = descriptor;
            }
            node = node.getOrCreate(items[c], itemLength, itemDescriptor);
        }
    }

    public void add(Node node) {
        node.setParent(this);
        put(node.getName(), node);
    }

    public Node getOrCreate(String name, long length, long descriptor) {
        Node node = get(name);
        if (node == null) {
            node = new Node(this, name, length, file, descriptor);
            add(node);
        }
        return node;
    }

    public Collection<Node> list() {
        return values();
    }

    public void walk(WalkCallback callback) {
        walk(PATH_SEPARATOR, callback);
    }

    private void walk(String path, WalkCallback callback) {
        if (!isEmpty()) {
            path += getName() + PATH_SEPARATOR;
            List<Pair<String, Node>> pairList = new ArrayList<>(values().size());
            for (Node node : values()) {
                if (node.isEmpty()) {
                    pairList.add(new Pair<>(path, node));
                }
            }
            Collections.sort(pairList, new Comparator<Pair<String, Node>>() {
                @Override
                public int compare(Pair<String, Node> o1, Pair<String, Node> o2) {
                    return o1.getValue().getName().compareToIgnoreCase(o2.getValue().getName());
                }
            });
            for (Pair<String, Node> pair : pairList) {
                callback.onNode(pair.getKey(), pair.getValue());
            }

            pairList.clear();

            for (Node node : values()) {
                if (!node.isEmpty()) {
                    pairList.add(new Pair<>(path + node.getName() + PATH_SEPARATOR, node));
                }
            }
            Collections.sort(pairList, new Comparator<Pair<String, Node>>() {
                @Override
                public int compare(Pair<String, Node> o1, Pair<String, Node> o2) {
                    return o1.getKey().compareToIgnoreCase(o2.getKey());
                }
            });
            for (Pair<String, Node> pair : pairList) {
                callback.onPath(pair.getKey());
            }
            for (int c = pairList.size() - 1; c >= 0; c--) {
                Pair<String, Node> pair = pairList.get(c);
                Node value = pair.getValue();
                value.walk(path, callback);
            }
        }
    }

    public void delete() throws IOException {
        DataInputStream stream = null;
        RandomAccessFile raf = null;
        try {
            InputStream inputStream = new FileInputStream(file);
            skipStream(descriptor, inputStream);
            stream = new DataInputStream(new BufferedInputStream(inputStream));
            BagFile bagFile = readBagFile(stream);
            skipStream(length, stream);

            raf = new RandomAccessFile(file, "rw");
            raf.seek(descriptor);
            long appended = 0;
            int read;
            while ((read = stream.read(buffer)) != -1) {
                raf.write(buffer, 0, read);
                appended += read;
            }
            raf.getChannel().truncate(descriptor + appended);

            parent.remove(getName());
            final long delta = bagFile.getInternal();
            parent.walk(new WalkCallback() {
                @Override
                public void onNode(String path, Node node) {
                    if (node.getDescriptor() > descriptor) {
                        node.moveDescriptor(-delta);
                    }
                }

                @Override
                public void onPath(String path) {
                }
            });
        } finally {
            safeClose(stream);
            safeClose(raf);
        }
    }

    public InputStream getInputStream() throws IOException {
        InputStream inputStream = new FileInputStream(file);
        skipStream(descriptor, inputStream);
        DataInputStream stream = new DataInputStream(new BufferedInputStream(inputStream));
        BagFile bagFile = readBagFile(stream);
        return bagFile.getStream();
    }

    public static Node scan(File file) throws IOException {
        final Node node = new Node();
        node.setFile(file);
        node.setName(FilesHelper.getFileBaseFromName(file.getName()));
        read(new FileInputStream(file), new BagCallback() {

            @Override
            public boolean onFile(String path, long length, long read, InputStream stream) throws IOException {
                skipStream(length, stream);
                node.add(path, length, read);
                return false;
            }
        });
        return node;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private static void read(InputStream inputStream, BagCallback callback) throws IOException {
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(new BufferedInputStream(inputStream));
            long read = 0;
            boolean eof = false;
            do {
                try {
                    BagFile bagFile = readBagFile(stream);
                    if (callback.onFile(bagFile.getPath(), bagFile.getLength(), read, bagFile.getStream())) {
                        eof = true;
                    }
                    read += bagFile.getInternal();
                } catch (EOFException ex) {
                    eof = true;
                }
            } while (!eof);
        } finally {
            safeClose(stream);
        }
    }

    private static BagFile readBagFile(DataInputStream stream) throws IOException {
        stream.mark(2);
        short pathLength = stream.readShort();
        stream.reset();
        String path = stream.readUTF();
        long length = stream.readLong();
        long internal = 2 + pathLength + 8 + length;
        return new BagFile(path, length, new LimitedInputStream(stream, length), internal);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static void skipStream(long length, InputStream stream) throws IOException {
        long skipped = 0;
        if (length == 0) {
            return;
        }
        while ((skipped += stream.skip(length - skipped)) < length) ;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void saveFile(File file, InputStream stream) throws IOException {
        if (file.exists()) {
            file.delete();
        } else {
            file.getParentFile().mkdirs();
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            int read;
            while ((read = stream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } finally {
            safeClose(outputStream);
        }
    }

    private interface BagCallback {

        boolean onFile(String path, long length, long read, InputStream stream) throws IOException;
    }

    public interface WalkCallback {

        void onNode(String path, Node node);

        void onPath(String path);
    }
}
