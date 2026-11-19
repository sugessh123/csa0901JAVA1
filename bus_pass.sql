-- Local TEST database created only for this assignment.
CREATE DATABASE IF NOT EXISTS busdb_test;
USE busdb_test;

DROP TABLE IF EXISTS bus_pass;
CREATE TABLE bus_pass (
    pass_id  VARCHAR(12) PRIMARY KEY,
    reg_no   VARCHAR(15) NOT NULL,
    name     VARCHAR(60) NOT NULL,
    route_no INT         NOT NULL,
    amount   DOUBLE      NOT NULL
) ENGINE=InnoDB;   -- InnoDB is required: MyISAM ignores transactions
