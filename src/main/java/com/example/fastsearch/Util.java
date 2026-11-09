package com.example.fastsearch;

public final class Util {
    private Util() {}

    public static String size(long b) {
        if (b >= 1L << 30) return String.format("%.1f GB", b / 1073741824.0);
        if (b >= 1L << 20) return String.format("%.1f MB", b / 1048576.0);
        if (b >= 1L << 10) return String.format("%.0f KB", b / 1024.0);
        return b + " B";
    }

    public static String ext(String name) {
        int d = name.lastIndexOf('.');
        return d < 0 ? "" : name.substring(d + 1).toLowerCase();
    }
}