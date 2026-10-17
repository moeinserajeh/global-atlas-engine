package com.globalatlas.map;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/v1/map")
public class MapFeatureController {
 private static final double MAX_TOLERANCE=0.5;
 private final JdbcTemplate jdbcTemplate;
 public MapFeatureController(JdbcTemplate jdbcTemplate){this.jdbcTemplate=jdbcTemplate;}
 @GetMapping(value="/countries",produces="application/geo+json")
 public String countryFeatures(@RequestParam(defaultValue="0.02") double tolerance){
  double t=validate(tolerance);
  return jdbcTemplate.queryForObject("""
   SELECT json_build_object('type','FeatureCollection','features',COALESCE(json_agg(json_build_object(
   'type','Feature','id',id,'geometry',ST_AsGeoJSON(CASE WHEN ?=0 THEN geometry ELSE ST_SimplifyPreserveTopology(geometry,?) END)::json,
   'properties',json_build_object('id',id,'iso2',iso2,'iso3',iso3,'nameEn',name_en,'nameFa',name_fa))),'[]'::json))::text
   FROM countries WHERE geometry IS NOT NULL
   """,String.class,t,t);
 }
 @GetMapping(value="/countries/{iso2}/regions",produces="application/geo+json")
 public String regionFeaturesByCountry(@PathVariable String iso2,@RequestParam(defaultValue="0.02") double tolerance){
  if(iso2==null||!iso2.matches("(?i)[a-z]{2}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ISO-3166 alpha-2 code required");
  double t=validate(tolerance);
  return jdbcTemplate.queryForObject("""
   SELECT json_build_object('type','FeatureCollection','features',COALESCE(json_agg(json_build_object(
   'type','Feature','id',r.id,'geometry',ST_AsGeoJSON(CASE WHEN ?=0 THEN r.geometry ELSE ST_SimplifyPreserveTopology(r.geometry,?) END)::json,
   'properties',json_build_object('id',r.id,'code',r.code,'nameEn',r.name_en,'nameFa',r.name_fa,'countryId',r.country_id,'adminLevel',r.admin_level))),'[]'::json))::text
   FROM administrative_regions r JOIN countries c ON c.id=r.country_id WHERE c.iso2=? AND r.geometry IS NOT NULL
   """,String.class,t,t,iso2.toUpperCase(Locale.ROOT));
 }
 @GetMapping(value="/regions",produces="application/geo+json")
 public String regionFeatures(@RequestParam(defaultValue="0.02") double tolerance,@RequestParam(defaultValue="1000") int limit){
  double t=validate(tolerance); if(limit<1||limit>5000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"limit must be 1..5000");
  return jdbcTemplate.queryForObject("""
   SELECT json_build_object('type','FeatureCollection','features',COALESCE(json_agg(json_build_object(
   'type','Feature','id',r.id,'geometry',ST_AsGeoJSON(CASE WHEN ?=0 THEN r.geometry ELSE ST_SimplifyPreserveTopology(r.geometry,?) END)::json,
   'properties',json_build_object('id',r.id,'countryId',r.country_id,'code',r.code,'nameEn',r.name_en,'nameFa',r.name_fa,'adminLevel',r.admin_level))),'[]'::json))::text
   FROM (SELECT id,country_id,code,name_en,name_fa,admin_level,geometry FROM administrative_regions WHERE geometry IS NOT NULL ORDER BY id LIMIT ?) r
   """,String.class,t,t,limit);
 }
 private static double validate(double v){if(!Double.isFinite(v)||v<0||v>MAX_TOLERANCE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"tolerance must be between 0 and 0.5 degrees"); return v;}
}
