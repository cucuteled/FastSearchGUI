package com.example.fastsearch;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public record SearchFilter(String name, Set<String> exts, boolean ignoreExts,
                           long minBytes, long maxBytes,
                           long modifiedFrom, long modifiedTo) {   // [from, to)

    public static SearchFilter fromState(ConfigState s) {
        String name = s.isContainsNameAllowed ? s.containsName.toLowerCase(Locale.ROOT) : "";

        Set<String> exts = new HashSet<>();
        if (s.isSpecifyFileTypeAllowed)
            for (String e : s.FileTypes) exts.add(e.toLowerCase(Locale.ROOT).replaceFirst("^\\.", ""));

        long min = s.FileSizeMinimum * 1024 * 1024;
        long max = s.FileSizeMaximum * 1024 * 1024;

        long from = Long.MIN_VALUE, to = Long.MAX_VALUE;
        if (s.isDateAllowed && s.fileDateCondition != null) {
            ZoneId z = ZoneId.systemDefault();
            if (s.isDateConditionSpecifyBeforeDate)
                to = s.fileDateCondition.atStartOfDay(z).toInstant().toEpochMilli();
            else
                from = s.fileDateCondition.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli();
        }
        return new SearchFilter(name, exts, s.isFileTypeModeIgnore, min, max, from, to);
    }

    public boolean test(Result r) {
        if (!name.isEmpty() && !r.getName().toLowerCase(Locale.ROOT).contains(name)) return false;
        if (!exts.isEmpty()) {
            boolean in = exts.contains(Util.ext(r.getName()));
            if (in == ignoreExts) return false;
        }
        if (minBytes > 0 && r.getSize() < minBytes) return false;
        if (maxBytes > 0 && r.getSize() > maxBytes) return false;
        long m = r.getModified();
        return m >= modifiedFrom && m < modifiedTo;
    }
}