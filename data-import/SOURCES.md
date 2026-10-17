# Geographic data sources and attribution

Primary source: [geoBoundaries API](https://www.geoboundaries.org/api.html)

The importer uses the `gbOpen` product and requests ADM0 and ADM1. geoBoundaries documents gbOpen as CC BY 4.0 compliant and requires attribution. Individual records contain their own source and license metadata; these values are preserved in the database. Do not treat all underlying source licenses as identical without checking the returned metadata.

Suggested dataset-level attribution:

> Administrative boundaries: geoBoundaries Database (www.geoboundaries.org). See per-layer source and license metadata stored with each imported record.

Before production release:
- Run the importer and archive its report.
- Verify ISO-2 mapping coverage and failed layers.
- Verify the boundary-year coverage and geometry validity.
- Display appropriate attribution in the app's About/Data Sources screen.
- Re-run imports only after reviewing upstream changes and migration behavior.
