package com.example.fastsearch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigState {

    public final AtomicInteger activeSearchThreads = new AtomicInteger(0);
    //
    public List<Result> results = new ArrayList<>();
    public int ProcessedFiles = 0;
    //
    public final List<String> DISALLOWED_PATH_SEGMENTS = List.of(
            "\\windows\\",
            "\\program files\\",
            "\\program files (x86)\\",
            "\\programdata\\",
            "\\system volume information\\",
            "\\$recycle.bin\\",
            "\\pagefile.sys",
            "\\swapfile.sys",
            "\\recovery\\",
            "\\perflogs\\",
            "\\boot\\",
            "\\$windows.~bt\\",
            "\\windows.old\\",
            "\\programdata\\microsoft\\vault\\",   // védett
            "\\system32\\drivers\\",
            "\\drivers\\",
            "\\msocache\\"
    );

    //

    public boolean isSpecifiedLocation;
    public String selectedPath;
    public List<String> selectedDrives = new ArrayList<>();

    // search

    public boolean isContainsNameAllowed = false;
    public String containsName = "";

    public boolean isSpecifyFileTypeAllowed = false;
    public boolean isFileTypeModeIgnore = false;
    public List<String> FileTypes = new ArrayList<>();

    public boolean isAttrAllowed = false;
    public long FileSizeMaximum = 0; // in bytes
    public long FileSizeMinimum = 0; // in bytes

    public boolean isDateAllowed = false;
    public LocalDate fileDateCondition;
    public boolean isDateConditionSpecifiyBeforeDate = false;

    // render

    public boolean Descending = true;
    public String orderMode = "orderByName";
}