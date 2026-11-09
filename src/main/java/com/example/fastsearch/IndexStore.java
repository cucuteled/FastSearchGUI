package com.example.fastsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// thread safe
public final class IndexStore {
    private final ArrayList<Result> all = new ArrayList<>();

    public final AtomicLong processed = new AtomicLong();
    public final AtomicInteger activeTasks = new AtomicInteger();
    public final List<ResultDir> roots = new CopyOnWriteArrayList<>();
    public volatile String lastPath = "";
    public volatile boolean finished;

    public void addAll(List<Result> batch) {
        if (batch.isEmpty()) return;
        synchronized (all) { all.addAll(batch); }
    }

    public List<Result> slice(int from) {
        synchronized (all) {
            return new ArrayList<>(all.subList(Math.min(from, all.size()), all.size()));
        }
    }

    public int size() { synchronized (all) { return all.size(); } }
}