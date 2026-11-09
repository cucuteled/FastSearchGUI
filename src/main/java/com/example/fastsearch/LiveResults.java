package com.example.fastsearch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LiveResults {
    private final IndexStore index;
    private final List<Result> matched = new ArrayList<>();
    private SearchFilter filter;
    private Comparator<Result> cmp;
    private boolean ascending;

    private int scanned;
    private boolean dirty = true, forced = true;
    private long lastSort;

    public LiveResults(IndexStore index, SearchFilter f, Comparator<Result> cmp, boolean asc) {
        this.index = index; this.filter = f; this.cmp = cmp; this.ascending = asc;
    }

    public void setFilter(SearchFilter f) {
        filter = f; matched.clear(); scanned = 0; dirty = true; forced = true;
    }

    public void setOrder(Comparator<Result> c, boolean asc) {
        cmp = c; ascending = asc; dirty = true; forced = true; lastSort = 0;
    }

    public boolean pump(boolean finished) {
        boolean changed = forced;
        forced = false;

        List<Result> fresh = index.slice(scanned);
        scanned += fresh.size();
        for (Result r : fresh) {
            if (!r.isDeleted() && filter.test(r)) { matched.add(r); dirty = true; changed = true; }
        }

        long now = System.currentTimeMillis();
        if (dirty && (finished || matched.size() < 20_000 || now - lastSort > 1000)) {
            matched.sort(ascending ? cmp : cmp.reversed());
            dirty = false; lastSort = now; changed = true;
        }
        return changed;
    }

    public int size() { return matched.size(); }
    public Result get(int i) { return matched.get(i); }
    public void remove(Result r) { matched.remove(r); }
}