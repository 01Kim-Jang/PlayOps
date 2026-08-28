package com.playops.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class BoardPostSchemaReconciler implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BoardPostSchemaReconciler.class);

    private final JdbcTemplate jdbcTemplate;

    public BoardPostSchemaReconciler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        reconcilePostTypeConstraint();
    }

    private void reconcilePostTypeConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE board_posts DROP CONSTRAINT IF EXISTS board_posts_type_check");
            jdbcTemplate.execute("""
                    ALTER TABLE board_posts
                    ADD CONSTRAINT board_posts_type_check
                    CHECK (type IN ('REQUEST', 'QUESTION', 'FAQ', 'FREE', 'NOTICE', 'ETC'))
                    """);
        } catch (Exception e) {
            log.debug("Board post type check constraint reconciliation skipped: {}", e.getMessage());
        }
    }
}
