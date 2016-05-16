package com.tomclaw.bag;

import java.io.*;
import java.util.List;

/**
 * Simplest more to one file storage.
 * Created by solkin on 12/05/16.
 */
public class Bag {

    private static final int BUFFER_SIZE = 10240;

    private final byte[] buffer;
    private final File bagFile;

    public Bag(String path) {
        buffer = new byte[BUFFER_SIZE];
        bagFile = new File(path);
    }

    public void pack(String rootPath, List<File> files, BagProgressCallback callback) throws IOException {
        DataOutputStream stream = null;
        int count = 0;
        long size = 0;
        try {
            File outputDir = bagFile.getParentFile();
            outputDir.mkdirs();
            if (bagFile.exists()) {
                bagFile.delete();
            }
            stream = new DataOutputStream(new FileOutputStream(bagFile));
            for (File file : files) {
                String pathDelta = file.getParent().substring(rootPath.length());
                File deltaFile = new File(pathDelta, file.getName());
                long length = file.length();
                stream.writeUTF(deltaFile.getAbsolutePath());
                stream.writeLong(length);
                InputStream inputStream = null;
                try {
                    inputStream = new LimitedInputStream(new FileInputStream(file), length);
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        stream.write(buffer, 0, read);
                    }
                } catch (Throwable ex) {
                    System.out.println(file.getName());
                    throw ex;
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                // System.out.println("~> " + deltaFile.getAbsolutePath() + " (" + length + " bytes)");
                count++;
                size += length;
                callback.onProgress(count * 100 / files.size());
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
        System.out.println(count + " files, " + size + " bytes total size");
    }

    public Node scan() throws IOException {
        Node node = new Node("/", -1);

        read(new BagCallback() {
            @Override
            public void onFile(String path, long length, long size, InputStream stream) throws IOException {
                skipStream(length, stream);
                node.add(path, length);
            }
        });
        return node;
    }

    public void unpack(final String output, BagProgressCallback callback) throws IOException {
        read(new BagCallback() {

            long total = 0;

            @Override
            public void onFile(String path, long length, long size, InputStream stream) throws IOException {
                Bag.this.saveFile(new File(output, path), length, stream);
                total += length;
                callback.onProgress((int) (total * 100 / size));
            }
        });
    }

    public void unpack(final String name, UnpackCallback callback) throws IOException {
        read(new BagCallback() {
            @Override
            public void onFile(String path, long length, long size, InputStream stream) throws IOException {
                if (FilesHelper.getFileName(path).equals(name)) {
                    callback.onFile(path, length, stream);
                } else {
                    skipStream(length, stream);
                }
            }
        });
    }

    public void unpack(final String output, final String name) throws IOException {
        read(new BagCallback() {
            @Override
            public void onFile(String path, long length, long size, InputStream stream) throws IOException {
                File file = new File(output, path);
                if (file.getName().equalsIgnoreCase(name)) {
                    Bag.this.saveFile(new File(output, path), length, stream);
                } else {
                    skipStream(length, stream);
                }
            }
        });
    }

    private void read(BagCallback callback) throws IOException {
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(new FileInputStream(bagFile));
            long size = bagFile.length();
            boolean eof = false;
            do {
                try {
                    String path = stream.readUTF();
                    long length = stream.readLong();
                    // System.out.println("<~ " + path + " (" + length + " bytes)");
                    callback.onFile(path, length, size, new LimitedInputStream(stream, length));
                } catch (EOFException ex) {
                    eof = true;
                }
            } while (!eof);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void skipStream(long length, InputStream stream) throws IOException {
        long skipped = 0;
        if (length == 0) {
            return;
        }
        while ((skipped += stream.skip(length - skipped)) < length) ;
    }

    private void saveFile(File file, long length, InputStream stream) throws IOException {
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
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public String getName() {
        return bagFile.getName();
    }

    private interface BagCallback {

        void onFile(String path, long length, long size, InputStream stream) throws IOException;
    }

    private interface UnpackCallback {

        void onFile(String path, long length, InputStream stream) throws IOException;
    }

    public interface BagProgressCallback {

        void onProgress(int percent);
    }
}
