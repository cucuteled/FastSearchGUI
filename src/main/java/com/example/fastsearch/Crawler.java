package com.example.fastsearch;

import com.example.fastsearch.ResultDir.VolumeInfo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public final class Crawler {
    private final IndexStore idx;
    private final ExecutorService ex;
    private final List<String> skip;

    public Crawler(IndexStore idx, ExecutorService ex, List<String> skip) {
        this.idx = idx; this.ex = ex; this.skip = skip;
    }

    public void startAll(List<Path> paths) {
        idx.activeTasks.incrementAndGet();           // őr: addig ne legyen 0, amíg minden gyökeret el nem indítottunk
        try {
            for (Path p : paths) {
                ResultDir rd = new ResultDir(p, null);
                rd.setVolume(VolumeInfo.of(p));
                idx.roots.add(rd);
                submit(rd);
            }
        } finally {
            release();
        }
    }

    private void submit(ResultDir d) {
        idx.activeTasks.incrementAndGet();           // a submit ELŐTT növelünk
        try {
            ex.execute(() -> {
                try { scan(d); }
                catch (Throwable ignored) { }
                finally { d.markDone(); release(); }
            });
        } catch (RejectedExecutionException e) {
            release();
        }
    }

    private void release() {
        if (idx.activeTasks.decrementAndGet() == 0) {
            idx.roots.forEach(ResultDir::finalizeSizes);   // alulról felfelé összesítés
            idx.finished = true;
        }
    }

    private boolean disallowed(Path p) {
        String s = p.toString().toLowerCase(Locale.ROOT);
        if (!s.endsWith("\\")) s += "\\";
        for (String seg : skip) if (s.contains(seg)) return true;
        return false;
    }

    private void scan(ResultDir d) {
        if (disallowed(d.getPath())) return;
        idx.lastPath = d.getPath().toString();

        List<Result> batch = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(d.getPath())) {
            for (Path p : ds) {
                BasicFileAttributes a;
                try { a = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); }
                catch (IOException | SecurityException e) { continue; }

                if (a.isRegularFile()) {
                    batch.add(new Result(p.getFileName().toString(), a.size(), a.lastModifiedTime().toMillis(), d));
                    d.addOwn(a.size());
                } else if (a.isDirectory()) {
                    ResultDir c = new ResultDir(p, d);
                    d.addChild(c);
                    submit(c);
                } else {
                    // junction / symlink / mount pont: csak akkor megyünk be, ha MÁSIK kötet (ciklusvédelem)
                    try {
                        if (Files.isDirectory(p)
                                && !Files.getFileStore(p).equals(Files.getFileStore(d.getPath()))) {
                            ResultDir c = new ResultDir(p, d);
                            c.markMount(VolumeInfo.of(p));
                            d.addChild(c);
                            submit(c);
                        }
                    } catch (IOException | SecurityException ignored) { }
                }
            }
        } catch (IOException | SecurityException ignored) { }

        idx.addAll(batch);
        idx.processed.addAndGet(batch.size() + 1);
    }
}