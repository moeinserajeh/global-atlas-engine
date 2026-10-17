package com.globalatlas.region;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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
                        rs.getLong("id"), rs.getLong("country_id"), rs.getString("code"),
                        rs.getString("name_en"), rs.getString("name_fa"), rs.getInt("admin_level")
                ),
                iso2.toUpperCase(Locale.ROOT), limit, offset
        );
    }

    public List<RegionSearchResult> search(String rawQuery, int limit) {
        String query = normalize(rawQuery);
        String pattern = "%" + escapeLike(query) + "%";
        return jdbcTemplate.query("""
                SELECT r.id, c.iso2 AS country_iso2, c.name_en AS country_name_en,
                       c.name_fa AS country_name_fa, r.code, r.name_en, r.name_fa, r.admin_level
                FROM administrative_regions r
                JOIN countries c ON c.id = r.country_id
                WHERE lower(r.name_en) LIKE lower(?) ESCAPE '\\'
                   OR lower(coalesce(r.name_fa, '')) LIKE lower(?) ESCAPE '\\'
                   OR lower(coalesce(r.code, '')) LIKE lower(?) ESCAPE '\\'
                ORDER BY CASE
                    WHEN lower(r.name_en) = lower(?) OR lower(coalesce(r.name_fa, '')) = lower(?) THEN 0
                    WHEN lower(r.name_en) LIKE lower(?) ESCAPE '\\' THEN 1
                    ELSE 2
                END, r.name_en
                LIMIT ?
                """,
                (rs, rowNum) -> new RegionSearchResult(
                        rs.getLong("id"), rs.getString("country_iso2"),
                        rs.getString("country_name_en"), rs.getString("country_name_fa"),
                        rs.getString("code"), rs.getString("name_en"), rs.getString("name_fa"),
                        rs.getInt("admin_level")
                ),
                pattern, pattern, pattern, query, query, escapeLike(query) + "%", limit
        );
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
                .replace('ي', 'ی')
                .replace('ك', 'ک');
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
