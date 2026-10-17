package com.globalatlas.region;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RegionRepository {
    private final JdbcTemplate jdbcTemplate;

    public RegionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RegionSummary> findByCountryIso2(String iso2, int limit, int offset) {
        return jdbcTemplate.query("""
                SELECT r.id, r.country_id, r.code, r.name_en, r.name_fa, r.admin_level
                FROM administrative_regions r
                JOIN countries c ON c.id = r.country_id
                WHERE c.iso2 = ?
                ORDER BY r.admin_level, r.name_en
                LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> new RegionSummary(
                        rs.getLong("id"),
                        rs.getLong("country_id"),
                        rs.getString("code"),
                        rs.getString("name_en"),
                        rs.getString("name_fa"),
                        rs.getInt("admin_level")
                ),
                iso2.toUpperCase(), limit, offset
        );
    }
}
