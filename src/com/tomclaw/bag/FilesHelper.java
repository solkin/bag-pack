package com.tomclaw.bag;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by solkin on 15/05/16.
 */
public class FilesHelper {

    public static List<File> listFiles(File source, FilenameFilter filter) {
        List<File> allFiles = new ArrayList<>();
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    allFiles.addAll(listFiles(file, filter));
                } else {
                    if (filter.accept(file.getParentFile(), file.getName())) {
                        allFiles.add(file);
                    }
                }
            }
        }
        return allFiles;
    }

    public static String getFileExtension(String fileName) {
        String extension = "";

        int i = fileName.lastIndexOf('.');
        int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        if (i > p) {
            extension = fileName.substring(i+1);
        }
        return extension;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return String.format("%d bytes", bytes);
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KiB", bytes / 1024.0f);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MiB", bytes / 1024.0f / 1024.0f);
        } else {
            return String.format("%.3f GiB", bytes / 1024.0f / 1024.0f / 1024.0f);
        }
    }

    public static String getFileBaseFromName(String name) {
        String base = name;
        if (!TextUtils.isEmpty(name)) {
            int index = name.lastIndexOf(".");
            if (index != -1) {
                base = name.substring(0, index);
            }
        }
        return base;
    }

    public static String getFileName(String path) {
        String name = path;
        if (!TextUtils.isEmpty(path)) {
            int index = path.lastIndexOf("/");
            if (index != -1) {
                name = name.substring(index + 1);
            }
        }
        return name;
    }

    public static String getFileExtensionFromPath(String path) {
        String suffix = "";
        if (!TextUtils.isEmpty(path)) {
            int index = path.lastIndexOf(".");
            if (index != -1) {
                suffix = path.substring(index + 1);
            }
        }
        return suffix;
    }
}
