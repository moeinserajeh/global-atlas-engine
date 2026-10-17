package com.globalatlas.country;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CountryRepository {
    private final JdbcTemplate jdbcTemplate;

    public CountryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CountrySummary> findAll(int limit, int offset) {
        return jdbcTemplate.query("""
                SELECT id, iso2, iso3, name_en, name_fa
                FROM countries
                ORDER BY name_en
                LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> new CountrySummary(
                        rs.getLong("id"),
                        rs.getString("iso2").trim(),
                        rs.getString("iso3").trim(),
                        rs.getString("name_en"),
                        rs.getString("name_fa")
                ),
                limit, offset
        );
    }

    public List<CountrySummary> search(String query, int limit) {
        String pattern = "%" + query.toLowerCase() + "%";
        return jdbcTemplate.query("""
                SELECT id, iso2, iso3, name_en, name_fa
                FROM countries
                WHERE lower(name_en) LIKE ? OR lower(coalesce(name_fa, '')) LIKE ?
                   OR lower(iso2) = ? OR lower(iso3) = ?
                ORDER BY name_en
                LIMIT ?
                """,
                (rs, rowNum) -> new CountrySummary(
                        rs.getLong("id"),
                        rs.getString("iso2").trim(),
                        rs.getString("iso3").trim(),
                        rs.getString("name_en"),
                        rs.getString("name_fa")
                ),
                pattern, pattern, query.toUpperCase(), query.toUpperCase(), limit
        );
    }

    public Optional<CountrySummary> findByIso2(String iso2) {
        return jdbcTemplate.query("""
                SELECT id, iso2, iso3, name_en, name_fa
                FROM countries
                WHERE iso2 = ?
                """,
                (rs, rowNum) -> new CountrySummary(
                        rs.getLong("id"),
                        rs.getString("iso2").trim(),
                        rs.getString("iso3").trim(),
                        rs.getString("name_en"),
                        rs.getString("name_fa")
                ),
                iso2.toUpperCase()
        ).stream().findFirst();
    }
}
