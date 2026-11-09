package com.example.fastsearch;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;

public final class Result {
    private final String name;
    private final long size;
    private final long modified; // epoch ms
    private final ResultDir parent;
    private volatile boolean deleted;

    public Result(String name, long size, long modified, ResultDir parent) {
        this.name = name;
        this.size = size;
        this.modified = modified;
        this.parent = parent;
    }

    public String getName()        { return name; }
    public long getSize()          { return size; }
    public long getModified()      { return modified; }
    public ResultDir getParent()   { return parent; }
    public String getPath()        { return parent.getPath().resolve(name).toString(); }
    public boolean isDeleted()     { return deleted; }
    public void markDeleted()      { deleted = true; }

    public LocalDate getDate() {
        return Instant.ofEpochMilli(modified).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Comparator<Result> comparator(String mode) {
        switch (mode) {
            case "orderByTime": return Comparator.comparingLong(Result::getModified);
            case "orderBySize": return Comparator.comparingLong(Result::getSize);
            case "orderByType": return Comparator.comparing((Result r) -> Util.ext(r.name))
                    .thenComparing(r -> r.name, String.CASE_INSENSITIVE_ORDER);
            default:            return Comparator.comparing((Result r) -> r.name, String.CASE_INSENSITIVE_ORDER);
        }
    }
}