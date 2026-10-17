package com.globalatlas.region;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/regions")
public class RegionDetailController {
    private final JdbcTemplate jdbcTemplate;
    public RegionDetailController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Region id must be positive");
        return jdbcTemplate.query("""
            SELECT r.id, r.code, r.name_en AS "nameEn", r.name_fa AS "nameFa",
                   r.admin_level AS "adminLevel", c.iso2 AS "countryIso2",
                   c.name_en AS "countryNameEn", c.name_fa AS "countryNameFa",
                   r.data_source AS "dataSource", r.data_license AS "dataLicense",
                   r.dataset_version AS "datasetVersion", r.reference_date AS "referenceDate",
                   CASE WHEN r.geometry IS NULL THEN false ELSE ST_IsValid(r.geometry) END AS "geometryValid",
                   CASE WHEN r.geometry IS NULL THEN NULL ELSE ST_Area(r.geometry::geography) / 1000000.0 END AS "areaKm2"
            FROM administrative_regions r JOIN countries c ON c.id=r.country_id WHERE r.id=?
            """, rs -> {
            if (!rs.next()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("id", rs.getLong("id"));
            result.put("code", rs.getString("code"));
            result.put("nameEn", rs.getString("nameEn"));
            result.put("nameFa", rs.getString("nameFa"));
            result.put("adminLevel", rs.getInt("adminLevel"));
            result.put("countryIso2", rs.getString("countryIso2").trim());
            result.put("countryNameEn", rs.getString("countryNameEn"));
            result.put("countryNameFa", rs.getString("countryNameFa"));
            result.put("dataSource", rs.getString("dataSource"));
            result.put("dataLicense", rs.getString("dataLicense"));
            result.put("datasetVersion", rs.getString("datasetVersion"));
            result.put("referenceDate", rs.getDate("referenceDate") == null ? null : rs.getDate("referenceDate").toString());
            result.put("geometryValid", rs.getBoolean("geometryValid"));
            result.put("areaKm2", rs.getObject("areaKm2") == null ? null : rs.getDouble("areaKm2"));
            return result;
        }, id);
    }
}
