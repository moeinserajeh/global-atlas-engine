# Geographic data sources and attribution

Primary source: [geoBoundaries API](https://www.geoboundaries.org/api.html), product \`gbOpen\`, administrative levels \`ADM0\` and \`ADM1\`.

geoBoundaries documents gbOpen as CC BY 4.0 compliant and requires attribution. Individual records may expose different source, license, build date, and boundary-year metadata. The importer persists these per-layer values; do not treat the entire dataset as having identical source lineage without checking each record.

Suggested dataset-level attribution:

> Administrative boundaries: geoBoundaries Database (www.geoboundaries.org). See per-layer source and license metadata stored with each imported record.

Release checklist:
- Run the importer and archive its JSON run report and CSV coverage report.
- Review every failed layer and ISO-2 mapping exception.
- Review country/ADM1 counts, geometry validity, SRID and boundary-year coverage.
- Confirm whether missing ADM1 is genuinely unavailable upstream or an import failure.
- Display attribution in the app's About/Data Sources screen.
- Re-run imports after reviewing upstream metadata changes. The importer is upsert-based and does not automatically delete obsolete rows.
