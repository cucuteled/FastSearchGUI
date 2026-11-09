package com.example.fastsearch;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class Result implements Comparable<Result> {

    private final File jFile;

    private final String fileName;
    private final long size;

    private final String filePath;
    private final ResultDir parentFolder;

    private final LocalDate lastModifiedDate;

    public Result(File file, ResultDir parent) {
        this.jFile    = file;
        this.fileName = file.getName();
        this.size     = file.length();
        parent.reportSize(size);
        this.filePath = file.getAbsolutePath();
        this.parentFolder = parent;
        this.lastModifiedDate = Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    @Override
    public int compareTo(Result o) {
        switch (FSApplication.getInstance().STATE.orderMode) {
            case "orderByTime":
                return getLastModified().compareTo(o.getLastModified());
            case "orderBySize":
                if (getBytes() == o.getBytes()) {
                    return 0;
                } else {
                    return getBytes() > o.getBytes() ? 1 : -1;
                }
            case "orderByType":
                String ext1 = getFileName();
                int dot1 = ext1.lastIndexOf(".");
                ext1 = (dot1 == -1) ? "" : ext1.substring(dot1 + 1).toLowerCase();

                String ext2 = o.getFileName();
                int dot2 = ext2.lastIndexOf(".");
                ext2 = (dot2 == -1) ? "" : ext2.substring(dot2 + 1).toLowerCase();

                return ext1.compareTo(ext2);
            default:
                return getFileName().compareTo(o.getFileName());
        }
    }

    public LocalDate getLastModified() {
        return this.lastModifiedDate;
    }

    public long getBytes() {
        return this.size;
    }

    public String getFileName() {
        return this.fileName;
    }

    public File getjFile() {
        return jFile;
    }

    public String getPath() {
        return filePath;
    }

    public ResultDir getParentFolder() {
        return parentFolder;
    }
}
