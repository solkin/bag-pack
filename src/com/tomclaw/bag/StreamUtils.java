package com.tomclaw.bag;

import java.io.Closeable;
import java.io.IOException;

public class StreamUtils {

    public static void safeClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

}
