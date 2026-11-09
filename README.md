\# FastSearch



!\[FastSearch](rimg.png)



FastSearch is a high-performance, multi-threaded file search application for Windows, built with JavaFX.  

It scans the filesystem in parallel, applies advanced filtering rules, and displays results with a modern and responsive UI.



---



\## Features



\### Multi-Threaded File Searching

\- Each directory is processed on its own thread.

\- Automatic tracking of active search threads.

\- Live progress indicator showing processed file count.

\- Intelligent timeout detection if scanning stalls.



\### Search Filters

FastSearch supports layered filtering conditions, including:



\- \*\*File Name\*\* — partial matching supported.

\- \*\*File Types\*\* — allowlist or denylist of extensions.

\- \*\*File Size\*\* — minimum and maximum byte-based limit.

\- \*\*Modified Date\*\* — show files \*before\* or \*after\* a specific date.

\- \*\*Protected / System Folder Exclusion\*\*  

&nbsp; (e.g. Windows, Program Files, System32, temp folders, etc.)



\### Sorting Options

Results can be sorted by:

\- Name  

\- Size  

\- Modified Date  

\- File Type  



Each sort mode supports ascending or descending order.



\### Rich Results View

Each result entry includes:

\- File icon (type-aware)  

\- File name  

\- File size (MB)  

\- Last modified date  

\- \*\*Open\*\* button — opens the file in Windows Explorer with selection  

\- \*\*Delete\*\* button — deletes the file after a confirmation dialog  



---



\## Installation \& Usage



\### 1. Download

A pre-built executable (fat JAR) can be downloaded here at releases.



\### 2. Run

Since FastSearch is delivered as a \*\*fat JAR\*\*, it includes JavaFX and requires only a standard Java runtime:

## Version 2 – Changelog

### Highlights
- **Everything is indexed.** The search no longer filters while scanning: all files are indexed,
  and filters are applied on top of the index. The wizard settings are only the *initial* filter.
- **Live filtering.** Change the name, file types or min/max size at any time. The new filter is
  applied instantly to everything indexed so far, and to new files as they are discovered.
- **Virtualized results.** Only the rows that fit on screen (plus one partial row) are rendered,
  so millions of results stay smooth.
- **Folder tree view (new).** Switch between *List* and *Tree* at any time.

### Results list
- 3-column layout, filled top to bottom, column by column; scroll by column with the mouse wheel
  or the scrollbar.
- Compact icon buttons (Open / Delete) in the top-right corner of every entry; long file names
  shrink with an ellipsis instead of hiding the buttons.
- Full path in a tooltip. File icons are cached.
- Fixed sort direction toggle (ascending / descending).

### Tree view
- Flowgraph-style folder tree with right-angle ("Manhattan") arrows.
- Zoom (mouse wheel, centered on the cursor) and pan (drag).
- Left click expands / collapses a folder, right click opens it in Explorer.
- Folders are expanded by default, largest first, up to a node limit to keep the graph readable.
- Every folder shows its total size (KB / MB / GB), aggregated bottom-up through the whole subtree.
- **Heat map:** folders and arrows are colored from yellow (light) to red (heavy), relative to
  the total capacity of the volume, so large hidden branches stand out immediately.
- One tree per selected drive, side by side. Each root shows the volume info
  (file system, free / total space). Folders mounted from another volume are marked with ⛁
  and are not counted into their parent's size.

### Progress & stability
- Animated ASCII gear tied to the processed file count.
- Stall detection: if there is no progress for 5 seconds, the gear stops and shows the last
  folder being scanned.
- Reworked crawler: one task per directory on a shared thread pool, reliable completion
  detection (no more timeout workaround), thread-safe index, daemon threads.
- Junctions and symbolic links are no longer followed, which prevents infinite loops.
- Reduced memory usage: results no longer hold `File` objects, paths are built on demand.
- Fixed icon lookup for files without an extension, and protected folders are now matched
  correctly (the folder itself, not only its children).

### Notes
- The search window is now resizable (default 1100×700).
- The wizard and options screens are unchanged.




