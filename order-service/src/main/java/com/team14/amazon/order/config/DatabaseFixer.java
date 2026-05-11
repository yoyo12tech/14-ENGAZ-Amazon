package com.team14.amazon.order.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseFixer {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void fixDatabaseConstraints() {
        try {
            jdbcTemplate.execute("ALTER TABLE products DROP CONSTRAINT IF EXISTS products_category_check");
            jdbcTemplate.execute("ALTER TABLE products DROP CONSTRAINT IF EXISTS products_status_check");
            jdbcTemplate.execute("ALTER TABLE shipments DROP CONSTRAINT IF EXISTS shipments_status_check");
        } catch (Exception e) {
            // Ignore if it fails
        }
    }
}
