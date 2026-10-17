package com.globalatlas.country;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
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
                SELECT c.id, c.iso2, c.iso3, c.name_en, c.name_fa, c.capital,
                       c.population, c.area_km2,
                       (SELECT count(*) FROM administrative_regions r WHERE r.country_id=c.id) AS region_count,
                       c.data_source, c.data_license, c.dataset_version, c.reference_date
                FROM countries c
                ORDER BY c.name_en
                LIMIT ? OFFSET ?
                """, this::mapCountry, limit, offset);
    }

    public List<CountrySummary> search(String rawQuery, int limit) {
        String query = normalize(rawQuery);
        String pattern = "%" + escapeLike(query) + "%";
        return jdbcTemplate.query("""
                SELECT c.id, c.iso2, c.iso3, c.name_en, c.name_fa, c.capital,
                       c.population, c.area_km2,
                       (SELECT count(*) FROM administrative_regions r WHERE r.country_id=c.id) AS region_count,
                       c.data_source, c.data_license, c.dataset_version, c.reference_date
                FROM countries c
                WHERE lower(c.name_en) LIKE lower(?) ESCAPE '\\'
                   OR lower(coalesce(c.name_fa, '')) LIKE lower(?) ESCAPE '\\'
                   OR lower(c.iso2) = ? OR lower(c.iso3) = ?
                ORDER BY CASE
                    WHEN lower(c.name_en)=lower(?) OR lower(coalesce(c.name_fa,''))=lower(?) THEN 0
                    WHEN lower(c.name_en) LIKE lower(?) ESCAPE '\\' THEN 1
                    ELSE 2
                END, c.name_en
                LIMIT ?
                """, this::mapCountry,
                pattern, pattern, query.toUpperCase(Locale.ROOT), query.toUpperCase(Locale.ROOT),
                query, query, escapeLike(query) + "%", limit);
    }

    public Optional<CountrySummary> findByIso2(String rawIso2) {
        return jdbcTemplate.query("""
                SELECT c.id, c.iso2, c.iso3, c.name_en, c.name_fa, c.capital,
                       c.population, c.area_km2,
                       (SELECT count(*) FROM administrative_regions r WHERE r.country_id=c.id) AS region_count,
                       c.data_source, c.data_license, c.dataset_version, c.reference_date
                FROM countries c
                WHERE c.iso2 = ?
                """, this::mapCountry, rawIso2.toUpperCase(Locale.ROOT)).stream().findFirst();
    }

    private CountrySummary mapCountry(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CountrySummary(
                rs.getLong("id"),
                rs.getString("iso2").trim(),
                rs.getString("iso3").trim(),
                rs.getString("name_en"),
                rs.getString("name_fa"),
                rs.getString("capital"),
                (Long) rs.getObject("population"),
                rs.getBigDecimal("area_km2"),
                rs.getLong("region_count"),
                rs.getString("data_source"),
                rs.getString("data_license"),
                rs.getString("dataset_version"),
                rs.getObject("reference_date", java.time.LocalDate.class)
        );
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFKC)
                .replace('ي', 'ی')
                .replace('ك', 'ک')
                .toLowerCase(Locale.ROOT);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
