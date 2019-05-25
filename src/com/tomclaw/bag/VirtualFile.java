package com.tomclaw.bag;

import java.io.File;

/**
 * Created by solkin on 16/05/16.
 */
@SuppressWarnings("WeakerAccess")
public class VirtualFile extends File {

    private String path;
    private final long length;

    public VirtualFile(String path, long length) {
        super(path);
        this.path = path;
        this.length = length;
    }

    @Override
    public boolean isDirectory() {
        return length == -1;
    }

    @Override
    public boolean isFile() {
        return length >= 0;
    }

    @Override
    public String getAbsolutePath() {
        return path;
    }

    @Override
    public long length() {
        return length;
    }
}
