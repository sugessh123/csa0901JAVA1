Task B2 – Adversarial inputs (local TEST database only).

1. Start MySQL and run `setup_users.sql` (creates database `busdb_test` and table `users`).
2. Put the MySQL Connector/J jar (for example `mysql-connector-j-8.x.jar`) in this folder.
3. If your MySQL password is not empty: `export DB_PASSWORD=yourpassword` (Windows: `set DB_PASSWORD=yourpassword`).
4. Compile and run (Linux/Mac use `:`, Windows use `;` in the classpath):
   `javac *.java`
   `java -cp .:mysql-connector-j-8.x.jar LoginDemo`
   `java DamagedFileDemo`   (needs no database)

`damaged_file_output.txt` is real output of `DamagedFileDemo`. `expected_output_LoginDemo.txt` shows the expected behaviour; take your own screenshot of the real run.
