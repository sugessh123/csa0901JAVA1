# CO5 Java Assignment 5 – Data Persistence Engineering (Transport Office Bus-Pass System)

**Student Name:** [Enter your name]  
**Register Number:** [Enter your register number]  
**Java Version:** JDK 11 or later  
**Repository name:** `CSA09-<RegisterNumber>-Assignment5`

## Project contents
- `Assignment_Report.md` – answers and explanations for Part A (A1–A5) and Part B (B1–B4).
- `TaskA1` … `TaskA5` – short README per design task (pseudocode is in the report).
- `TaskB1/PassStore.java` – completed "Complete the Code" program.
- `TaskB1/PassFileStore.java`, `TaskB1/B1Experiments.java` – full file store, tests and timing experiments.
- `TaskB2/` – `setup_users.sql`, `LoginDemo.java` (unsafe vs safe login), `DamagedFileDemo.java`.
- `TaskB3/` – `bus_pass.sql`, `PassLoader.java` (completed), `LoaderBenchmark.java`.
- `TaskB4/` – `Migration.java` (file → MySQL migration, failure report, DatabaseMetaData).

## What has been run and what you must run
| Part | Status when this package was made |
|---|---|
| B1 (`PassStore`, `B1Experiments`) | Compiled and **run**; real results are in the report and `TaskB1/measurements_run1.txt`. |
| B2 `DamagedFileDemo` | Compiled and **run**; output in `TaskB2/damaged_file_output.txt`. |
| B2 `LoginDemo`, B3, B4 (need MySQL) | **Compiled only.** `PassLoader` transaction logic was checked with a mock connection. You must run them against your own MySQL test database and fill in the timing tables. |

## MySQL setup (for Tasks B2, B3, B4)
1. Install MySQL and create the test database only for this assignment (the `.sql` files do this).
2. Download MySQL Connector/J (`mysql-connector-j-8.x.jar`) and put it in each task folder, or add it to the classpath.
3. Connection settings are in `Db.java`; to avoid editing code, set environment variables `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## How to compile and run
Open a terminal in the task folder. On Windows replace `:` in the classpath with `;`.

### Task B1
```bash
cd TaskB1
javac *.java
java PassStore
java B1Experiments 50
```

### Task B2
```bash
cd TaskB2
# run setup_users.sql in MySQL first
javac *.java
java -cp .:mysql-connector-j-8.x.jar LoginDemo
java DamagedFileDemo
```

### Task B3
```bash
cd TaskB3
# run bus_pass.sql in MySQL first
javac *.java
java -cp .:mysql-connector-j-8.x.jar LoaderBenchmark
```

### Task B4
```bash
cd TaskB4
# run bus_pass.sql in MySQL first
javac *.java
java -cp .:mysql-connector-j-8.x.jar Migration
```

## Before submitting
1. Replace the student name, register number and GitHub link in this file and in the report.
2. Run all programs on your computer. Replace the B1 timings in the report with your own, and fill in the B3 tables.
3. Save your own screenshots of the outputs in the task folders. Do not claim screenshots you have not taken.
4. Create the GitHub repository `CSA09-<RegisterNumber>-Assignment5`, share it with the faculty, and make at least one commit for each task (the commit history is checked).
5. Add the repository link to the report cover page and to the assignment sheet.

**Notes:** In the assignment text the multi-catch in `PassLoader` is printed as `SQLException │ RuntimeException` (a special bar character); in Java it is the normal `|`. Part A must be submitted as pseudocode, not Java code – it is in `Assignment_Report.md`. The `.sql` files and Db.java use a local **test** database only; never use real passwords or data.
