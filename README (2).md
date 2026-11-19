Task B3 – Statement vs PreparedStatement vs Batch (local TEST database only).

1. Run `bus_pass.sql` in MySQL (creates `busdb_test.bus_pass`, InnoDB).
2. Put the MySQL Connector/J jar in this folder; set `DB_PASSWORD` if your password is not empty.
3. Compile and run:
   `javac *.java`
   `java -cp .:mysql-connector-j-8.x.jar LoaderBenchmark`        (1,000 / 10,000 / 100,000 rows)
   `java -cp .:mysql-connector-j-8.x.jar LoaderBenchmark 10000`  (quicker: stops at 10,000 rows)
4. Copy the printed timing tables into Assignment_Report.md (B3.2 and B3.3) and save screenshots here.

`PassLoader.java` is the completed "Complete the Code" program (TODO 1-3).
`expected_output.txt` shows what to expect for the parts that do not depend on machine speed.
