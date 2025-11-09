package com.example.fastsearch;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class Result implements Comparable<Result> {

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

    private final File file;

    public Result(File file) {
        this.file = file;
    }

    public String getFileName() {
        return file.getName();
    }

    public LocalDate getLastModified() {
        return Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public long getBytes() {
        return file.length();
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

}
