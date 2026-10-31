package com.taskflow.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Configuration
public class DataSourceConfig {
    @Bean
    DataSource dataSource(Environment environment) {
        boolean testProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
        String jdbcUrl;
        String username;
        String password;

        if (testProfile) {
            jdbcUrl = environment.getProperty("spring.datasource.url",
                    "jdbc:h2:mem:taskflowtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
            username = "sa";
            password = "";
        } else {
            jdbcUrl = environment.getProperty("JDBC_DATABASE_URL");
            username = environment.getProperty("DB_USERNAME", "taskflow");
            password = environment.getProperty("DB_PASSWORD", "");

            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                String managedUrl = environment.getProperty("DATABASE_URL");
                if (managedUrl != null && !managedUrl.isBlank()) {
                    if (managedUrl.startsWith("jdbc:")) {
                        jdbcUrl = managedUrl;
                    } else {
                        ParsedUrl parsed = parseManagedUrl(managedUrl);
                        jdbcUrl = parsed.jdbcUrl();
                        username = parsed.username();
                        password = parsed.password();
                    }
                } else {
                    String host = environment.getProperty("DB_HOST", "localhost");
                    String port = environment.getProperty("DB_PORT", "3306");
                    String database = environment.getProperty("DB_NAME", "taskflow");
                    jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                }
            }
        }

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(jdbcUrl.startsWith("jdbc:h2:")
                ? "org.h2.Driver"
                : "com.mysql.cj.jdbc.Driver");
        dataSource.setMaximumPoolSize(10);
        return dataSource;
    }

    static ParsedUrl parseManagedUrl(String value) {
        int queryStart = value.indexOf('?');
        String address = queryStart >= 0 ? value.substring(0, queryStart) : value;
        String rawQuery = queryStart >= 0 ? value.substring(queryStart + 1) : null;
        URI uri;
        try {
            uri = URI.create(address.replaceFirst("^mysql://", "https://"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("DATABASE_URL must be a valid mysql:// URL");
        }

        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || path == null || path.length() < 2) {
            throw new IllegalArgumentException("DATABASE_URL must include a MySQL host and database name");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 3306;
        String database = path.substring(1);
        boolean local = host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1");
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + queryWithTls(rawQuery, local);

        String username = "taskflow";
        String password = "";
        String rawUserInfo = uri.getRawUserInfo();
        if (rawUserInfo != null) {
            String[] credentials = rawUserInfo.split(":", 2);
            username = decode(credentials[0]);
            if (credentials.length > 1) {
                password = decode(credentials[1]);
            }
        }
        return new ParsedUrl(jdbcUrl, username, password);
    }

    private static String queryWithTls(String rawQuery, boolean local) {
        List<String> parameters = new ArrayList<>();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String item : rawQuery.split("&")) {
                String key = item.split("=", 2)[0];
                String normalized = decode(key).toLowerCase(Locale.ROOT);
                // Managed Node-style DSNs can carry ssl={...}; Connector/J has its own sslMode option.
                if (!Set.of("ssl", "sslmode", "usessl", "requiressl", "verifyservercertificate").contains(normalized)) {
                    parameters.add(item);
                }
            }
        }
        parameters.add("sslMode=" + (local ? "DISABLED" : "VERIFY_IDENTITY"));
        return "?" + String.join("&", parameters);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    record ParsedUrl(String jdbcUrl, String username, String password) {}
}
