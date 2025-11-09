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

A pre-built executable (fat JAR) can be downloaded here:



\*\*\[[Download FastSearch](https://github.com/cucuteled/FastSearchGUI/raw/refs/heads/main/FastSearch-1.0.jar)]\*\*  

\*(Replace with your actual URL.)\*



\### 2. Run

Since FastSearch is delivered as a \*\*fat JAR\*\*, it includes JavaFX and requires only a standard Java runtime:





