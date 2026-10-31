package com.taskflow.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataSourceConfigTest {
    @Test
    void parsesManagedTlsDsnAndPreservesEncodedCredentials() {
        DataSourceConfig.ParsedUrl parsed = DataSourceConfig.parseManagedUrl(
                "mysql://alice:p%40ss%2Bword@db.example:4000/sample?ssl={\"rejectUnauthorized\":true}");

        assertEquals("jdbc:mysql://db.example:4000/sample?sslMode=VERIFY_IDENTITY", parsed.jdbcUrl());
        assertEquals("alice", parsed.username());
        assertEquals("p@ss+word", parsed.password());
    }
}
