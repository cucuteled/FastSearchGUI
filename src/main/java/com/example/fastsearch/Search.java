package com.example.fastsearch;

import javafx.application.Platform;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class Search {

    private final FSApplication app = FSApplication.getInstance();

    private final String wokingPath;
    private String[] files;

    private final ResultDir parent;

    public Search(ResultDir parent) {
        this.wokingPath = parent.getAbsPath();
        this.parent     = parent;

        // Normpath
        String normPath = wokingPath.toLowerCase();

        // Check if it's under disallowed routes
        boolean disallowed = false;
        for (String seg : app.STATE.DISALLOWED_PATH_SEGMENTS) {
            if (normPath.contains(seg)) {
                disallowed = true;
                app.STATE.activeSearchThreads.decrementAndGet();
                break;
            }
        }

        if (!disallowed) {
            File dir = new File(wokingPath);
            if (dir.isDirectory()) {
                String[] listedFiles = dir.list();
                if (listedFiles != null) {
                    this.files = listedFiles;
                    Process();
                }
            }
        }
    }

    private void Process() {
        for (String f : files) {
            File file = new File(wokingPath + "\\" + f);

            // If file append to results as a file
            if (file.isFile()) {
//                boolean isValid = true;
//
//                boolean StrictFileName = app.STATE.isContainsNameAllowed;
//                boolean isSpecificFileType = app.STATE.isSpecifyFileTypeAllowed;
//                boolean isSpecificSizeMax = app.STATE.FileSizeMaximum != 0;
//                boolean isSpecificSizeMin = app.STATE.FileSizeMinimum != 0;
//                boolean isDateAllowed = app.STATE.isDateAllowed;
//
//                if (StrictFileName) {
//                    String fullName = file.getName();
//                    int dot = fullName.lastIndexOf('.');
//
//                    String nameOnly = (dot == -1) ? fullName : fullName.substring(0, dot);
//
//                    if (!nameOnly.toLowerCase().contains(app.STATE.containsName.toLowerCase()))
//                        isValid = false;
//                }
//
//
//                // FILE TYPE
//                if (isSpecificFileType) {
//                    String name = file.getName();
//                    int dot = name.lastIndexOf(".");
//                    String filetype = (dot == -1) ? "" : name.substring(dot + 1).toLowerCase();
//
//                    if (!filetype.isEmpty()) {
//                        if (app.STATE.isFileTypeModeIgnore) {
//                            if (app.STATE.FileTypes.contains(filetype))
//                                isValid = false;
//                        } else {
//                            if (!app.STATE.FileTypes.contains(filetype))
//                                isValid = false;
//                        }
//                    } else {
//                        isValid = false;
//                    }
//                }
//
//                // SIZE MAX
//                if (isSpecificSizeMax)
//                    if (file.length() > (app.STATE.FileSizeMaximum * 1024 * 1024))
//                        isValid = false;
//
//                // SIZE MIN
//                if (isSpecificSizeMin)
//                    if (file.length() < (app.STATE.FileSizeMinimum * 1024 * 1024))
//                        isValid = false;
//
//                // DATE
//                if (isDateAllowed) {
//
//                    LocalDate fileDate = Instant.ofEpochMilli(file.lastModified())
//                            .atZone(ZoneId.systemDefault())
//                            .toLocalDate();
//
//                    LocalDate selectedDate = app.STATE.fileDateCondition;
//
//                    if (app.STATE.isDateConditionSpecifyBeforeDate) {
//                        if (!fileDate.isBefore(selectedDate))
//                            isValid = false;
//
//                    } else {
//                        if (!fileDate.isAfter(selectedDate))
//                            isValid = false;
//                    }
//                }
//                // KÉSZ
//                app.STATE.ProcessedFiles += 1;
//                if (isValid) {
//                    // HA VALID
//                    //System.out.println("Találat: " + file.getName());
//                    app.STATE.results.add(new Result(file));
//                }
                // method2:
                app.STATE.ProcessedFiles += 1;
                app.STATE.results.add(new Result(file, parent));
            }

            // If Directory, append it to dirs and start new search with new parent:
            if (file.isDirectory()) {
                ResultDir rd = new ResultDir(file, parent);
                app.STATE.dirs.add(rd);
                app.STATE.activeSearchThreads.incrementAndGet();
                app.executor.submit(() -> new Search(rd));
            }
        }
        app.STATE.activeSearchThreads.decrementAndGet();
    }
}

