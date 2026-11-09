package com.example.fastsearch;

import java.io.File;

public class ResultDir {

    private final File file;
    private final ResultDir parent;
    private final String absPath;
    private final String name;

    private long totalBytes = 0;

    public ResultDir(File file, ResultDir parent) {
        this.file = file;
        this.parent = parent;
        this.absPath = file.getAbsolutePath();
        this.name = file.getName();
    }

    public void reportSize(long bytes) {
        this.totalBytes += bytes;
    }

    public File getFile() {
        return file;
    }

    public ResultDir getParent() {
        return parent;
    }

    public String getAbsPath() {
        return absPath;
    }

    public String getName() {
        return name;
    }

    public long getTotalBytes() {
        return totalBytes;
    }
}
