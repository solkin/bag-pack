package com.tomclaw.bag;

import java.io.*;
import java.util.List;

/**
 * Simplest more to one file storage.
 * Created by solkin on 12/05/16.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Bag {

    private static final int BUFFER_SIZE = 102400;

    private final byte[] buffer;
    private final File bagFile;
    private final Node node;

    public Bag(String path) {
        buffer = new byte[BUFFER_SIZE];
        bagFile = new File(path);
        node = new Node();
    }

    public void pack(String rootPath, List<File> files, BagProgressCallback callback) throws IOException {
        pack(rootPath, files, false, callback);
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public void write(DataOutputStream output, String path, long length, InputStream input) throws IOException {
        output.writeUTF(path);
        output.writeLong(length);
        InputStream inputStream = null;
        try {
            inputStream = new LimitedInputStream(input, length);
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (Throwable ex) {
            System.out.println(path);
            throw ex;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void pack(String rootPath, List<File> files, boolean append, BagProgressCallback callback) throws IOException {
        DataOutputStream stream = null;
        int count = 0;
        long size = 0;
        try {
            File outputDir = bagFile.getParentFile();
            outputDir.mkdirs();
            if (!append && bagFile.exists()) {
                bagFile.delete();
            }
            stream = new DataOutputStream(new FileOutputStream(bagFile, append));
            for (File file : files) {
                String pathDelta = file.getParent().substring(rootPath.length());
                File deltaFile = new File(pathDelta, file.getName());
                long length = file.length();
                write(stream, deltaFile.getAbsolutePath(), length, new FileInputStream(file));
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
        node.clear();
        read(new BagCallback() {

            @Override
            public boolean onFile(String path, long length, long read, long total, InputStream stream) throws IOException {
                skipStream(length, stream);
                node.add(path, length, read);
                return false;
            }
        });
        return node;
    }

    public void unpack(Node node, final UnpackCallback callback) throws IOException {
        long descriptor = node.getDescriptor();
        if (descriptor >= 0) {
            System.out.println("descriptor: " + descriptor);
            InputStream inputStream = new FileInputStream(bagFile);
            skipStream(descriptor, inputStream);
            read(inputStream, new BagCallback() {
                @Override
                public boolean onFile(String path, long length, long read, long total, InputStream stream) throws IOException {
                    callback.onFile(path, length, stream);
                    return true;
                }
            });
        }
    }

    public void unpack(final String output, final BagProgressCallback callback) throws IOException {
        read(new BagCallback() {

            @Override
            public boolean onFile(String path, long length, long read, long total, InputStream stream) throws IOException {
                Bag.this.saveFile(new File(output, path), length, stream);
                callback.onProgress((int) (read * 100 / total));
                return false;
            }
        });
    }

    public void unpack(final String name, final UnpackCallback callback) throws IOException {
        read(new BagCallback() {
            @Override
            public boolean onFile(String path, long length, long read, long total, InputStream stream) throws IOException {
                if (FilesHelper.getFileName(path).equals(name)) {
                    callback.onFile(path, length, stream);
                } else {
                    skipStream(length, stream);
                }
                return false;
            }
        });
    }

    public void unpack(final String output, final String name) throws IOException {
        read(new BagCallback() {
            @Override
            public boolean onFile(String path, long length, long read, long total, InputStream stream) throws IOException {
                File file = new File(output, path);
                if (file.getName().equalsIgnoreCase(name)) {
                    Bag.this.saveFile(new File(output, path), length, stream);
                } else {
                    skipStream(length, stream);
                }
                return false;
            }
        });
    }

    private void read(BagCallback callback) throws IOException {
        read(new FileInputStream(bagFile), callback);
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private void read(InputStream inputStream, BagCallback callback) throws IOException {
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(new BufferedInputStream(inputStream));
            long read = 0;
            long total = bagFile.length();
            boolean eof = false;
            do {
                try {
                    stream.mark(2);
                    short pathLength = stream.readShort();
                    stream.reset();
                    String path = stream.readUTF();
                    long length = stream.readLong();
                    if (callback.onFile(path, length, read, total, new LimitedInputStream(stream, length))) {
                        eof = true;
                    }
                    read += 2 + pathLength + 8 + length;
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

    @SuppressWarnings("StatementWithEmptyBody")
    private static void skipStream(long length, InputStream stream) throws IOException {
        long skipped = 0;
        if (length == 0) {
            return;
        }
        while ((skipped += stream.skip(length - skipped)) < length) ;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "TryFinallyCanBeTryWithResources"})
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

    public File getBagFile() {
        return bagFile;
    }

    private interface BagCallback {

        boolean onFile(String path, long length, long read, long total, InputStream stream) throws IOException;
    }

    public interface UnpackCallback {

        void onFile(String path, long length, InputStream stream) throws IOException;
    }

    public interface BagProgressCallback {

        void onProgress(int percent);
    }
}
