package com.globalatlas.map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/map")
public class MapFeatureController {
    private final JdbcTemplate jdbcTemplate;

    public MapFeatureController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping(value = "/countries", produces = "application/geo+json")
    public String countryFeatures() {
        return jdbcTemplate.queryForObject("""
                SELECT json_build_object(
                    'type', 'FeatureCollection',
                    'features', COALESCE(json_agg(json_build_object(
                        'type', 'Feature',
                        'id', id,
                        'geometry', ST_AsGeoJSON(geometry)::json,
                        'properties', json_build_object(
                            'id', id, 'iso2', iso2, 'iso3', iso3,
                            'nameEn', name_en, 'nameFa', name_fa
                        )
                    )), '[]'::json)
                )::text
                FROM countries WHERE geometry IS NOT NULL
                """, String.class);
    }

    @GetMapping(value = "/regions", produces = "application/geo+json")
    public String regionFeatures() {
        return jdbcTemplate.queryForObject("""
                SELECT json_build_object(
                    'type', 'FeatureCollection',
                    'features', COALESCE(json_agg(json_build_object(
                        'type', 'Feature',
                        'id', r.id,
                        'geometry', ST_AsGeoJSON(r.geometry)::json,
                        'properties', json_build_object(
                            'id', r.id, 'countryId', r.country_id,
                            'code', r.code, 'nameEn', r.name_en,
                            'nameFa', r.name_fa, 'adminLevel', r.admin_level
                        )
                    )), '[]'::json)
                )::text
                FROM administrative_regions r WHERE r.geometry IS NOT NULL
                """, String.class);
    }
}
