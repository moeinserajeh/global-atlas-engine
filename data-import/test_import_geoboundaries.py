import unittest

from shapely.geometry import mapping, MultiPolygon, Polygon

from import_geoboundaries import (
    metadata_for,
    records_from_metadata,
    resolve_iso2,
    merged_polygon_geojson,
    metadata_values,
)


class MetadataParsingTests(unittest.TestCase):
    def test_accepts_geo_boundaries_list_shape(self):
        records = [{"boundaryISO": "IRN"}, {"boundaryISO": "USA"}]
        self.assertEqual(records_from_metadata(records), records)

    def test_accepts_geo_boundaries_object_shape(self):
        records = [{"boundaryISO": "IRN"}]
        self.assertEqual(records_from_metadata({"geoBoundaries": records}), records)

    def test_metadata_rejects_duplicate_iso3(self):
        # Avoid network access by replacing get_json at the module boundary.
        import import_geoboundaries as importer
        original = importer.get_json
        importer.get_json = lambda _url: [{"boundaryISO": "IRN"}, {"boundaryISO": "IRN"}]
        try:
            with self.assertRaisesRegex(ValueError, "Duplicate"):
                metadata_for("ADM0")
        finally:
            importer.get_json = original


class GeometryTests(unittest.TestCase):
    def test_unions_all_features_including_islands(self):
        first = {"type": "Feature", "properties": {}, "geometry": mapping(Polygon([(0, 0), (1, 0), (1, 1), (0, 1), (0, 0)]))}
        second = {"type": "Feature", "properties": {}, "geometry": mapping(Polygon([(2, 0), (3, 0), (3, 1), (2, 1), (2, 0)]))}
        import json
        merged = json.loads(merged_polygon_geojson([first, second]))
        self.assertEqual(merged["type"], "MultiPolygon")
        self.assertEqual(len(merged["coordinates"]), 2)

    def test_invalid_polygon_is_repaired(self):
        bowtie = Polygon([(0, 0), (2, 2), (0, 2), (2, 0), (0, 0)])
        feature = {"type": "Feature", "properties": {}, "geometry": mapping(bowtie)}
        result = merged_polygon_geojson([feature])
        self.assertIn('"type":"MultiPolygon"', result)

    def test_rejects_point_geometry(self):
        feature = {"type": "Feature", "properties": {}, "geometry": {"type": "Point", "coordinates": [0, 0]}}
        with self.assertRaisesRegex(ValueError, "Expected polygon"):
            merged_polygon_geojson([feature])


class MetadataValuesTests(unittest.TestCase):
    def test_preserves_source_license_version_and_year(self):
        values = metadata_values({
            "boundarySource": "Survey",
            "boundaryLicense": "CC BY 4.0",
            "buildDate": "2026-01-02",
            "boundaryYearRepresented": "2024",
        })
        self.assertEqual(values, ("Survey", "CC BY 4.0", "2026-01-02", "2024-01-01"))

    def test_iso2_resolves_from_reviewed_iso3_registry(self):
        feature = {"properties": {}}
        self.assertEqual(resolve_iso2({"boundaryISO": "IRN"}, feature), "IR")
        with self.assertRaisesRegex(ValueError, "No ISO-3166 mapping"):
            resolve_iso2({"boundaryISO": "ZZZ"}, feature)


if __name__ == "__main__":
    unittest.main()
