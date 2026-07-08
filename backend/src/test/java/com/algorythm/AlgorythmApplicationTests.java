package com.algorythm;

import com.algorythm.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AlgorythmApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndRunsFlywayMigrationsAgainstRealPostgres() {
        String name = jdbcTemplate.queryForObject(
                "select name from app_info order by id limit 1",
                String.class
        );

        assertThat(name).isEqualTo("algorythm");
    }
}