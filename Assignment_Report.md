# CO5 Java Assignment 5
## Data Persistence Engineering – Files and Databases (Transport Office Bus-Pass System)

**Student Name:** [Enter your name]  
**Register Number:** [Enter your register number]  
**GitHub Link:** [Paste your repository link]  
**Java Version:** JDK 11 or later (measurements below were taken on JDK 21)

> This is a simple student-level explanation. Read the answers, run the programs, and make sure you can explain them during the viva.
> **Honesty note:** The timing tables in B1 are real measurements from running `B1Experiments.java`. The MySQL tasks (B2, B3, B4) need a MySQL server, so their measurement tables are left for you to fill in from your own run. Replace the B1 numbers with your own machine's numbers too.

---

# PART A – PERSISTENCE DESIGN & PSEUDOCODE

## Task A1 – Design a File Format

### A1.1 – Represent the Record

Record fields (in order): pass ID, register number, name, route number, boarding stop, validity date, amount paid.

**(a) Delimited text file** – one record per line, UTF-8, comma as delimiter, no header line.

```text
P1001,192311001,Arun Kumar,12,Main Gate,2027-06-30,4500.0
```

| # | Field | Type | Note |
|---|---|---|---|
| 1 | passId | text | unique, e.g. P1001 |
| 2 | regNo | text | 9 digits |
| 3 | name | text | may contain a comma |
| 4 | route | int | route number |
| 5 | stop | text | may contain a comma |
| 6 | validity | text | ISO date yyyy-MM-dd |
| 7 | amount | double | rupees |

**Name containing the delimiter:** before writing, `%` is replaced by `%25` and `,` is replaced by `%2C` (in name and stop). When reading, the reverse replacement is done. So `Meena, K` is stored as `Meena%2C K` and the line still has exactly 7 fields.

**(b) Binary file with DataOutputStream** – fixed-size records, so no delimiter is needed.

| Field | How written | Bytes |
|---|---|---|
| passId | 10 chars with `writeChar` (padded with spaces) | 20 |
| regNo | 12 chars (padded) | 24 |
| name | 30 chars (padded) | 60 |
| route | `writeInt` | 4 |
| stop | 20 chars (padded) | 40 |
| validity | `writeLong` (epoch day) | 8 |
| amount | `writeDouble` | 8 |
| **Record size** | | **164** |

In the binary design a comma inside a name is just another character, because fields are found by position, not by a delimiter. A name longer than 30 characters must be rejected or cut.

### A1.2 – Design the Operations (text file)

**Stream choice:** the file is text and is processed line by line, so character streams are the natural choice. `BufferedWriter(FileWriter(file, true))` is used for adding (append mode, buffered). `BufferedReader(FileReader(file))` is used for find and list because it gives `readLine()` and reads in large blocks. `FileInputStream/FileOutputStream` are not used directly because they work on raw bytes and have no `readLine()`; I would have to decode characters and find line ends myself.

**Add a pass**
```text
START
IF any field is empty or route/amount is not a valid number
    RETURN error
END IF
line = passId , regNo , escape(name) , route , escape(stop) , validity , amount
OPEN BufferedWriter in append mode (create the file if missing)
WRITE line
WRITE newline
CLOSE writer                      (close flushes the buffer)
STOP
```

**Find a pass by ID**
```text
START
OPEN BufferedReader on the file
IF file does not exist
    RETURN not found
END IF
WHILE (line = readLine()) is not end-of-file
    fields = split line by comma (keep empty fields)
    IF number of fields is not 7
        SKIP this line (damaged)
        CONTINUE
    END IF
    IF fields[0] equals passId
        RETURN fields (unescape name and stop)
    END IF
END WHILE
CLOSE reader
RETURN not found
STOP
```

**List all passes on a route**
```text
START
create empty list result
OPEN BufferedReader on the file
WHILE (line = readLine()) is not end-of-file
    fields = split line by comma
    IF fields count is not 7 OR route is not an integer
        SKIP line
        CONTINUE
    END IF
    IF integer(fields[3]) equals route
        ADD fields to result
    END IF
END WHILE
CLOSE reader
RETURN result
STOP
```

### A1.3 – Design for Damaged Data

| Problem | How the design detects it | How it is handled |
|---|---|---|
| Damaged line | Wrong number of fields, empty line, route not an integer, amount not a number | The line is skipped, the reason is recorded in an error list, and the search continues. |
| Missing file | `FileNotFoundException` / `NoSuchFileException` when opening | find and list return "not found" / empty list; add creates the file. |
| Incomplete write (power failure) | The last line is cut, so it has too few fields or has no newline at the end | The cut line is rejected by the field-count check. Before appending, the program checks that the file ends with a newline; if not, it writes a newline first, so a new record is not glued to the broken one. For rewrites, the new data is written to a temporary file and then renamed over the old file. |

### Alternative: RandomAccessFile with fixed-length records

With fixed-length records of `recordSize` = 164 bytes, record `i` starts at byte `i × 164`. The program calls `seek(i × recordSize)` and reads one record with `readFully`. Add means seeking to the end and writing one record.

| | Delimited text | RandomAccessFile |
|---|---|---|
| Find by ID | O(n) scan (about 4,000 lines on average for 8,000 passes) | O(1) seek **if** the position is known, e.g. pass IDs are sequential so index = number − 1. If IDs are random, a separate index (HashMap) or binary search on a sorted file, O(log n), is needed. |
| Space | about 57 bytes per record, 8,000 × 57 ≈ 0.45 MB | 164 bytes per record, 8,000 × 164 ≈ 1.3 MB |
| Padding waste | none | with typical lengths (name about 14 of 30 chars, passId 5 of 10, stop 12 of 20, regNo 9 of 12) roughly 60 bytes per record is padding, about 39% (my estimate) |

The seek is faster, but the file is about three times bigger, is not human-readable, and long names do not fit.

---

## Task A2 – Don't Read What You Don't Need

### A2.1 – Predict the Cost
A 50 MB file has about 52,428,800 bytes. `FileInputStream.read()` returns one byte per call, so the program makes about **52.4 million** `read()` calls (plus one final call that returns −1). Each call goes through native code and, without a buffer, asks the operating system for one byte. A system call costs far more than a normal method call, so the program spends nearly all its time in call overhead and not in counting.

### A2.2 – Redesign Using Buffering
```text
START
count = 0
buf = new byte array of 8192 (or 65536) bytes
OPEN FileInputStream
LOOP
    n = read(buf)                 // fills up to 8192 bytes in one call
    IF n equals -1 THEN BREAK
    FOR i = 0 TO n - 1
        IF buf[i] equals newline THEN count = count + 1
    END FOR
END LOOP
CLOSE stream
RETURN count
STOP
```
`BufferedInputStream` does the same thing internally. With an 8 KB buffer, the operating system is called about 52,428,800 / 8,192 = **6,400** times instead of 52 million, which is 8,192 times fewer calls. The byte-by-byte loop in memory is still done, but it is very fast.

### A2.3 – When Buffering Does Not Help
If the program already reads large blocks, for example `read(byte[1_000_000])`, an extra buffer only adds one more copy. Buffering also gives little benefit when reading a few bytes at scattered positions (random access), because each refill reads a block that is mostly not used.

### Alternative: `Files.readAllLines()`
This reads the whole file into a `List<String>` and the line count is `list.size()`. It is shorter code, but for a 50 MB file it keeps about 1.8 million `String` objects in memory.

| | Buffered stream | readAllLines |
|---|---|---|
| Time | O(n) | O(n) |
| Memory | O(buffer size) = 8 KB | O(file size) – more than 50 MB of heap because of String objects |

Use the buffered stream for large files and for one-pass work such as counting. Use `readAllLines` for small files, or when the lines must be sorted or accessed many times.

---

## Task A3 – Discover the Data-Access Design Pattern

### A3.1 – Identify the Common Pattern
**Repeated in all three classes:** open a connection → create a statement from an SQL string → set the parameters → execute → loop over the `ResultSet` and build objects → close everything → handle `SQLException`.
**Different in each class:** the table name, the SQL text, the parameters, and the code that converts one row into a `Student`, `Course` or `Marks` object.

### A3.2 – Design a Generic Data-Access Interface
```text
INTERFACE Dao<T, K>
    insert(T item)
    findById(K id)      RETURNS T or null
    findAll()           RETURNS list of T
    update(T item)
    delete(K id)
END INTERFACE

CLASS StudentDao IMPLEMENTS Dao<Student, String>
    insert(s):    run "INSERT INTO student(reg_no,name,dept,year,cgpa) VALUES(?,?,?,?,?)"
                  with s.regNo, s.name, s.dept, s.year, s.cgpa
    findById(id): run "SELECT * FROM student WHERE reg_no = ?" with id
                  IF a row exists RETURN mapRow(row) ELSE RETURN null
    findAll():    run "SELECT * FROM student"
                  FOR each row ADD mapRow(row) to list; RETURN list
    update(s):    run "UPDATE student SET name=?,dept=?,year=?,cgpa=? WHERE reg_no=?"
    delete(id):   run "DELETE FROM student WHERE reg_no = ?"
    mapRow(row):  RETURN new Student(row.reg_no, row.name, row.dept, row.year, row.cgpa)
END CLASS
```
The repeated steps (open, prepare, execute, close) can be written once in a helper or base class. `StudentDao`, `CourseDao` and `MarksDao` then supply only the SQL and `mapRow`.

### A3.3 – Map Objects to Tables
(The assignment does not list the Student fields, so I assumed these.)

| Student field | Java type | Column | SQL type |
|---|---|---|---|
| regNo | String | reg_no | VARCHAR(15) **PRIMARY KEY** |
| name | String | name | VARCHAR(60) |
| dept | String | dept | VARCHAR(30) |
| year | int | year | INT |
| cgpa | double | cgpa | DOUBLE |

### Alternative: SQL written directly in service/GUI classes
If a column is renamed (for example `reg_no` to `register_no`), with a generic Dao only the **one** `StudentDao` class changes. With SQL spread over service and GUI classes, **every** class that contains that SQL must be found and changed (k places), and one missed place fails only at run time. The cost of one database operation is the same in both designs, O(1) extra work and the same memory; the Dao adds only one extra method call, which is negligible compared with the database round trip. The difference is in maintenance and risk, not speed.

---

## Task A4 – What Should Be Serialized?

### A4.1 – Decide What to Persist
- `password` must be `transient`: a secret must not be written in plain form to a file.
- The database `Connection` must be `transient`: it is a live resource, it is not `Serializable` (so serializing it throws `NotSerializableException`), and a connection saved yesterday is meaningless after restart.
- `name` and `registerNumber` are normal persistent fields.

**Object graph:** when the `Hostel` object is written, Java also writes every object reachable from it (the list, each `Room`, each `Resident`). So `Hostel`, `Room` and `Resident` must all implement `Serializable`. An object that is referenced twice is written once and shared references are kept, and cycles are handled. When read back, transient fields come back as defaults (`null`, `0`), and the constructors of the serializable classes are not run.

### A4.2 – Plan for Change
When an old file (without `phone`) is read by the new class:
- **With `serialVersionUID` declared and unchanged:** the read works. The missing `phone` field gets the default value `null`, so the code must handle `null`.
- **Without a declared `serialVersionUID`:** Java computes it from the class structure. Adding a field changes the computed value, so reading the old file throws **`InvalidClassException`**.

So declare `private static final long serialVersionUID = 1L;` and change it only when the change is truly incompatible.

### Alternative: CSV or JSON text
| | Java serialization | CSV | JSON |
|---|---|---|---|
| File size for n residents | compact data plus class description (written once) and per-object overhead | smallest text | largest (field names repeated in every record) |
| Save / load time | O(n) | O(n) | O(n), plus parsing cost |
| New field added | works if serialVersionUID is fixed; new field is null | old rows have fewer columns, so the loader must supply a default | missing key is easy to treat as default |
| Other | only Java can read it; reading untrusted files is a security risk | human-readable | human-readable, works with other languages |

---

## Task A5 – Design for Correctness and Safety

### A5.1 – Transaction Design
```text
START
SET autoCommit = false
TRY
    rows1 = UPDATE wallet SET balance = balance - 500
            WHERE owner = 'hostel' AND balance >= 500
    IF rows1 is not 1
        ROLLBACK
        RETURN "insufficient balance"
    END IF
    rows2 = UPDATE wallet SET balance = balance + 500 WHERE owner = 'canteen'
    IF rows2 is not 1
        ROLLBACK
        RETURN "canteen wallet not found"
    END IF
    COMMIT
    RETURN "transfer done"
CATCH any error
    ROLLBACK
    RETURN "transfer failed"
FINALLY
    RESTORE the old autoCommit value
END TRY
STOP
```
If the program crashes between the two updates, the transaction is not committed. The database undoes it, so the ₹500 is neither lost nor created.

### A5.2 – Injection Analysis
For `SELECT * FROM users WHERE id='<id>' AND pwd='<pwd>'`:
- Input `id = admin' -- ` (any password) gives `... WHERE id='admin' -- ' AND pwd='...'`. Everything after `-- ` is a comment, so the password test disappears.
- Input `pwd = ' OR '1'='1` gives `... AND pwd='' OR '1'='1'`. The condition `'1'='1'` is always true, so rows are returned without the password.

The problem is that user input becomes part of the SQL text.

**Safe redesign**
```text
START
sql = "SELECT * FROM users WHERE id = ? AND pwd = ?"
ps = prepareStatement(sql)
ps.setString(1, id)
ps.setString(2, pwd)
rs = ps.executeQuery()
IF rs has a row RETURN login accepted ELSE RETURN login rejected
STOP
```
The SQL text is fixed and the input is sent only as data, so a quote is just a character. (In a real system the password must also be stored as a salted hash, not as plain text.)

### Alternative: two separate auto-commit statements
Each statement commits by itself. If the program fails after the first, the hostel wallet has lost ₹500 and the canteen wallet has not received it, so **₹500 disappears** and the data stays wrong. A transaction of two updates needs one commit; two auto-commit statements need two commits, so the transaction is not slower and is often a little faster. Both are O(1) in the number of rows. The real difference is correctness.

---

# PART B – IMPLEMENTATION & EXPERIMENTAL INVESTIGATION

## Task B1 – Implement and Instrument the File Store

**Code:** `TaskB1/PassStore.java` (completed "Complete the Code" program), `TaskB1/PassFileStore.java` (full store with escaping, validation and list), `TaskB1/B1Experiments.java` (tests and measurements).

**Completed TODO 1:** `find()` opens a `BufferedReader` in try-with-resources, reads each line, splits it with `split(",", -1)`, skips any line that does not have exactly 5 fields, and returns the fields when the first field equals the pass ID.

**Output of `java PassStore`** (matches the expected output):
```text
[P1003, 192311003, Divya, 7, 3800.0]
null
```
**Why the damaged line for P1002 does not stop the search:** the line `P1002,DAMAGED LINE` splits into only 2 fields. No exception is thrown; the `if (f.length != 5) continue;` statement skips to the next line, and the loop goes on until the end of the file. `P1003` is found after it. `find("P1002")` returns `null` because the damaged line is skipped and so never matches.

### B1.1 – Validate the Implementation (real output)

| Test | Input | Result | What it verifies |
|---|---|---|---|
| T1 | add + find P1001 | `[P1001, 192311001, Arun, 12, 4500.0]` | normal add and find work |
| T2 | find P1002, name "Meena, K" | `[P1002, 192311002, Meena, K, 7, 3800.0]` | comma in a name is escaped and restored correctly |
| T3 | find P9999 | `null` | a missing ID is reported as not found, without an error |
| T4 | find P1003 (damaged line `P1003,DAMAGED`) | `null` | a damaged record is not returned |
| T5 | find P1004 (after the damaged line) | `[P1004, 192311004, Divya, 12, 4200.0]` | the damaged line does not stop the search |
| T6 | list route 12 | `P1001 P1004` | list returns exactly the passes on the route and skips damaged lines |

### B1.2 – Compare with Your Design
1. The Part A design has 7 fields (with boarding stop and validity date). The given `PassStore` and the `bus_pass` table use 5 fields (no stop, no validity), so the implementation uses those 5. Adding the other two is only two more columns in the same format.
2. The given `add()` does not escape the delimiter. I added the `%2C` escaping from my design in `PassFileStore`, otherwise a name with a comma corrupts the line.
3. `split(",")` removes trailing empty fields, so `P1,192,Arun,5,` would look like 4 fields. I used `split(",", -1)`, which keeps empty fields so damaged lines are counted correctly. I found this only while implementing.
4. The design said "record the reason"; the implementation does this in `readAll()` (error list), while `find()` skips silently.

### B1.3 – Measure the Effect of Buffering (real measurements, JDK 21, Linux container)

| File size | Lines | Unbuffered `read()` (ms) | `byte[8192]` buffer (ms) | `BufferedReader` (ms) | `readAllLines` (ms) |
|---|---|---|---|---|---|
| 1 MB | 36,564 | 397.4 | 5.0 | 26.7 | 48.2 |
| 10 MB | 365,631 | 2,230.9 | 18.8 | 34.0 | 55.1 |
| 50 MB | 1,828,153 | 10,833.8 | 21.5 | 120.6 | 425.7 |

**Comparison with A2.1:** I predicted about 52 million calls and a very slow program. The unbuffered time grows in direct proportion to file size (about 0.2 µs per byte, roughly 10.8 s for 50 MB), which matches one call per byte. The 8 KB buffer was about **500 times faster** at 50 MB (21.5 ms against 10,834 ms), as expected from 8,192 times fewer calls. `BufferedReader` is slower than the raw byte buffer because it also decodes characters and creates a `String` per line, and `readAllLines` is the slowest of the buffered ones because it also keeps all lines in memory. (The 1 MB unbuffered value is higher per byte because of JVM warm-up.)

### B1.4 – Measure Growth (real measurements)

| Records | One find, last record (ms) | 100 finds (ms) | Time per find (ms) | HashMap: build + 100 lookups (ms) |
|---|---|---|---|---|
| 1,000 | 1.62 | 221.3 | 2.2 (JIT warm-up) | 3.5 |
| 5,000 | 5.93 | 103.8 | 1.0 | 6.9 |
| 10,000 | 1.46 | 161.7 | 1.6 | 3.9 |
| 50,000 | 16.58 | 757.1 | 7.6 | 42.8 |
| 100,000 | 32.08 | 1,370.2 | 13.7 | 54.0 |

**Trend:** from 10,000 records upward the time per find grows in proportion to the file size (50,000 to 100,000 records: 757 ms to 1,370 ms, about 1.8 times for 2 times the data). That is O(n), because the program must read every line up to the match. The small sizes are noisy because file opening and JVM warm-up dominate, so I use the 100-find column as the reliable one.

### Alternative: HashMap loaded at start-up
The file is read once into `HashMap<String, String[]>` keyed by pass ID.

| | Scan the file every time | HashMap loaded once |
|---|---|---|
| Time for q searches on n passes | O(q × n) | O(n + q) |
| Extra memory | O(1) (one line) | O(n): roughly 300 bytes per record, about 30 MB for 100,000 (estimate, not measured) |

Measured: at 100,000 records, 100 file searches took 1,370 ms, but building the map and doing 100 lookups took 54 ms. The map costs about 50 ms to build, which equals about 4 file searches, so after 4 searches the map is already cheaper. Its weakness: data added to the file later is not in the map until it is reloaded.

---

## Task B2 – Engineer an Adversarial Input

**Code:** `TaskB2/setup_users.sql`, `TaskB2/LoginDemo.java` (needs MySQL), `TaskB2/DamagedFileDemo.java` (runs without MySQL). A local database `busdb_test` is created only for this assignment.

### Unsafe login vs PreparedStatement
| Input (id / pwd) | SQL built by unsafe version | Unsafe | PreparedStatement |
|---|---|---|---|
| `admin` / `S3cret#99` | `... id='admin' AND pwd='S3cret#99'` | accepted | accepted |
| `admin` / `wrong` | `... id='admin' AND pwd='wrong'` | rejected | rejected |
| `admin' -- ` / `x` | `... id='admin' -- ' AND pwd='x'` | **accepted** (password test commented out) | rejected |
| `x` / `' OR '1'='1` | `... id='x' AND pwd='' OR '1'='1'` | **accepted** (condition always true) | rejected |

*These are the expected results from the SQL logic. Run `LoginDemo` against your MySQL test database and take a screenshot of the actual output.* The PreparedStatement version rejects both attacks because the whole input is compared as one text value; no user with that id or password exists.

### Damaged bus-pass file (real output of `DamagedFileDemo`)
```text
Valid records : 2
   [P2001, 192311001, Arun, 12, 4500.0]
   [P2007, 192311007, Sana, 3, 3500.0]
Rejected lines: 5
   line 2: expected 5 fields but found 4  -> [P2002,192311002,Divya,7]
   line 3: amount is not numeric: FORTY-FIVE  -> [P2003,192311003,Kavin,9,FORTY-FIVE]
   line 4: empty line  -> []
   line 5: route is not an integer: xx  -> [P2005,192311005,Meena,xx,3800.0]
   line 6: expected 5 fields but found 6  -> [P2006,192311006,Ravi,5,3000.0,EXTRA]
find(P2007) after the damaged lines -> [P2007, 192311007, Sana, 3, 3500.0]
```

### What each input reveals about the original design
- **SQL injection:** the original design mixes SQL code and user data in one string, so the user can change the meaning of the query. Data must travel separately from code.
- **Wrong field count, bad number, empty line:** the original `PassStore.find()` silently ignores such lines. That is safe for searching, but a damaged record disappears without anyone knowing, and the original `add()` has no validation, so bad data can enter the file in the first place. The improved store validates every field and reports the reason.

### Alternative: escaping quotes or validating with a regular expression
The program would reject or escape quote characters before building the SQL string (`regexLogin` in `LoginDemo`). This is weaker because: (1) it depends on the programmer thinking of every dangerous character and every SQL dialect and encoding trick; (2) it also rejects legitimate input (a password such as `it's me`); (3) one missed case, or a number field with no quotes, still allows injection. Validation costs O(L) for input length L, a few microseconds, so it is not a real saving: a PreparedStatement needs no validation and has about the same cost per query. Validation is still useful as an extra layer, but not as a replacement.

---

## Task B3 – Statement vs PreparedStatement vs Batch

**Code:** `TaskB3/bus_pass.sql`, `TaskB3/PassLoader.java` (completed), `TaskB3/LoaderBenchmark.java`. Use an **InnoDB** table (MyISAM ignores transactions). The MySQL URL uses `rewriteBatchedStatements=true`, which lets the driver send a batch as multi-row inserts.

**Completed TODOs in `PassLoader.loadAll`:**
- TODO 1: set the five parameters (`Integer.parseInt` for route, `Double.parseDouble` for amount), `addBatch()`, and `executeBatch()` after every 1,000 rows.
- TODO 2: `con.commit();` then `return n;`
- TODO 3: `con.rollback();` then `throw e;`

(The assignment text shows `SQLException │ RuntimeException` with a special bar character; in Java the multi-catch separator is the normal `|`.)

I checked the control flow of `loadAll` with a mock connection: for 2,500 good rows it did 3 batches then one commit; for a bad value at row 1,200 it threw `NumberFormatException` and did one rollback and no commit.

### B3.1 – Correctness on small data
`LoaderBenchmark` inserts 10 records with each approach and prints the row count (expected: 10 each time), then uses `ResultSetMetaData` to print the column names and types. Expected metadata: `pass_id VARCHAR → String`, `reg_no VARCHAR → String`, `name VARCHAR → String`, `route_no INT → Integer`, `amount DOUBLE → Double`.

### B3.2 – Increase the data size (fill in from your run)

| Rows | Statement (ms) | PreparedStatement (ms) | Prepared + Batch (ms) |
|---|---|---|---|
| 1,000 | ______ | ______ | ______ |
| 10,000 | ______ | ______ | ______ |
| 100,000 | ______ | ______ | ______ |

### B3.3 – Auto-commit vs transaction (fill in from your run)

| | Batch, auto-commit ON | Batch, one transaction |
|---|---|---|
| Time for 10,000 rows (ms) | ______ | ______ |
| Rows left after failure at row 5,001 | **5,000 expected** (earlier batches already committed) | **0 expected** (everything rolled back) |

The expected row counts follow from the code: with auto-commit, the 5 batches of 1,000 rows before the bad row are already committed; with the transaction, `rollback()` removes all of them.

### B3.4 – Engineering trade-off (what to expect and how to explain it)
- **Statement** is usually slowest: one network round trip per row, one commit per row, and the SQL text has to be built and parsed each time. It is also open to injection.
- **PreparedStatement** is safe and avoids rebuilding the SQL, but still makes one round trip and one commit per row, so it is only a little faster.
- **Batch** sends rows in groups of 1,000, so there are about 100 round trips instead of 100,000, and is the fastest.
- **A transaction** commits once instead of once per row, which saves the log flush for every row and also makes the load all-or-nothing.

Choice: **(a) nightly import of 100,000 passes** → PreparedStatement batches inside one transaction (as in `PassLoader`). **(b) a single pass at the counter** → one PreparedStatement insert; with one row, batching gives no benefit, and auto-commit is fine.

### Alternative: one multi-row INSERT or `LOAD DATA`
- **Multi-row INSERT** (`INSERT ... VALUES (...),(...),...`): time is O(n) with fewer round trips (similar to a rewritten batch). Client memory is O(b) for a batch of b rows because one big SQL string is built, and it is limited by `max_allowed_packet`.
- **LOAD DATA**: the server reads the file directly, so it is normally the fastest for very large loads. Client memory is about O(1) since the file is streamed, but it needs a file (and the `local_infile` setting), and it is harder to validate each row first.
- **PreparedStatement batch:** O(n) time, O(b) memory, and each row is validated in Java, which is why it suits this assignment. Compare your own measured times to confirm.

---

## Task B4 – Final Engineering Decision

**Code:** `TaskB4/Migration.java` (uses `PassFileStore.java`, `PassLoader.java`, `Db.java`).

### B4.1 – Implement a practical strategy
The program reads the existing pass file with `PassFileStore.readAll()`, which validates every line and separates valid rows from failed lines. The valid rows are loaded with `PassLoader.loadAll()` (batches of 1,000 inside one transaction). The failed lines are written to `failure_report.txt` and printed with line number and reason. The default sample file has 2,500 good lines and 2 bad lines, so the expected output is: *2,502 lines read, 2,500 valid, 2 failed; 2,500 rows loaded*; the report lists `P9001` (wrong field count) and `P9002` (amount not numeric). If the database load fails, nothing is committed.

### B4.2 – Inspect the database
`Migration` also uses `DatabaseMetaData` to print the database product and version, the driver name and version, the JDBC version, whether batch updates and transactions are supported, and the list of tables in `busdb_test`. This is useful when the application is moved to another server because the program can check that it is talking to the expected database and version, that the driver is compatible, that the needed features (transactions, batching) exist, and that the `bus_pass` table is present, before it starts a large migration.

### B4.3 – Production Decision
**Decision: use a relational database (MySQL) with JDBC.** Text files can stay for import/export and archives.

| Requirement | Text file | Serialization | Database |
|---|---|---|---|
| Search by pass ID at 100× scale | O(n); measured 13.7 ms per find at 100,000 records, which extrapolates to about 110 ms at 800,000 passes (my estimate, linear extrapolation) | must load the whole object into memory | primary-key index, O(log n) |
| List by route | full scan | full scan in memory | index on `route_no` |
| Several counters at once | two writers can interleave or damage lines; no locking | the whole file is rewritten on every save, so the last writer wins | row-level locking, safe concurrent inserts |
| Safety | partial write leaves a broken line | no protection against partial writes; version problems | transactions (commit/rollback), unique pass ID, constraints |
| 12 campuses sharing data | files must be copied around | each file is separate | one shared server |

My measurements support this: file search time grows in direct proportion to the data (B1.4), the in-memory HashMap fixes search speed but does nothing for concurrency or safety, and the transaction experiment (B3.3) shows the all-or-nothing behaviour that files cannot give.

### Alternative: each campus keeps its own serialized file
- **Search all passes:** the program must open and deserialize all 12 files, so time is O(N) for N total passes, and memory is O(N) if all are loaded together. A database index answers the same query in O(log N).
- **Space:** about the same total data, but pass records and objects are duplicated if a student is registered on several campuses.
- **Concurrent counters:** within one campus a `synchronized` block or file lock allows only one counter at a time, and each issued pass means rewriting the whole file, O(n) per pass. In the database, each insert is O(log n) and the server handles concurrent counters with row-level locks.
