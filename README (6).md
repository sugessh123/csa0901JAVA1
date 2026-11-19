Task B4 – Migration from the pass file to MySQL, failure report and DatabaseMetaData.

1. Run `bus_pass.sql` (same table as Task B3). Put the MySQL Connector/J jar in this folder; set `DB_PASSWORD` if needed.
2. Compile and run:
   `javac *.java`
   `java -cp .:mysql-connector-j-8.x.jar Migration`              (uses a generated sample file with 2 bad lines)
   `java -cp .:mysql-connector-j-8.x.jar Migration passes.csv`   (uses your own file)
3. `failure_report.txt` is created in the folder you run from. Save a screenshot of the console output.

The production decision (B4.3) and the alternative analysis are in Assignment_Report.md, Task B4.
