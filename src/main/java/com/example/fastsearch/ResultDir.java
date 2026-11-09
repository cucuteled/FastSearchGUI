package com.example.fastsearch;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ResultDir {

    public record VolumeInfo(String name, String type, long total, long free) {
        public static VolumeInfo of(Path p) {
            try {
                FileStore fs = Files.getFileStore(p);
                return new VolumeInfo(fs.name(), fs.type(), fs.getTotalSpace(), fs.getUsableSpace());
            } catch (IOException | RuntimeException e) {
                return null;
            }
        }
        public String describe(String rootPath) {
            return rootPath + "  ·  " + (name == null || name.isBlank() ? "" : name + "  ·  ")
                    + type + "  ·  " + Util.size(free) + " free / " + Util.size(total);
        }
    }

    private final Path path;
    private final ResultDir parent;
    private final String name;
    private final List<ResultDir> children = new ArrayList<>();

    private volatile long ownBytes;
    private volatile long totalBytes;  // all
    private volatile int fileCount;
    private volatile boolean done;

    private boolean mount; // other mount
    private VolumeInfo volume;

    public ResultDir(Path path, ResultDir parent) {
        this.path = path;
        this.parent = parent;
        Path fn = path.getFileName();
        this.name = fn == null ? path.toString() : fn.toString();   // on root its null
    }

    public void addOwn(long bytes) { ownBytes += bytes; fileCount++; }
    public void addChild(ResultDir c) { synchronized (children) { children.add(c); } }
    public void markDone() { done = true; }
    public void setVolume(VolumeInfo v) { this.volume = v; }
    public void markMount(VolumeInfo v) { this.mount = true; this.volume = v; }

    public long finalizeSizes() {
        long t = ownBytes;
        for (ResultDir c : childrenSnapshot()) {
            long s = c.finalizeSizes();
            if (!c.mount) t += s;
        }
        totalBytes = t;
        return t;
    }

    public List<ResultDir> childrenSnapshot() {
        synchronized (children) { return new ArrayList<>(children); }
    }
    public boolean hasChildren() { synchronized (children) { return !children.isEmpty(); } }

    public Path getPath()          { return path; }
    public ResultDir getParent()   { return parent; }
    public String getName()        { return name; }
    public long getOwnBytes()      { return ownBytes; }
    public long getTotalBytes()    { return totalBytes; }
    public int getFileCount()      { return fileCount; }
    public boolean isDone()        { return done; }
    public boolean isMount()       { return mount; }
    public VolumeInfo getVolume()  { return volume; }
}