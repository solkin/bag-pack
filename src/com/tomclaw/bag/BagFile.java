package com.tomclaw.bag;

/**
 * Created by solkin on 18/05/16.
 */
@SuppressWarnings("WeakerAccess")
public class BagFile {

    private final String path;
    private final long length;
    private final LimitedInputStream stream;
    private long internal;

    public BagFile(String path, long length, LimitedInputStream stream, long internal) {
        this.path = path;
        this.length = length;
        this.stream = stream;
        this.internal = internal;
    }

    public String getPath() {
        return path;
    }

    public long getLength() {
        return length;
    }

    public LimitedInputStream getStream() {
        return stream;
    }

    public long getInternal() {
        return internal;
    }
}
