# geoBoundaries data pipeline

Imports geoBoundaries gbOpen **ADM0 country outlines** and **ADM1 first-level administrative divisions** into PostgreSQL/PostGIS. It is repeatable for existing rows, validates/repairs polygon geometries, preserves per-layer source/license/version/reference year, and writes JSON and CSV coverage reports plus a database audit row.

Source documentation: https://www.geoboundaries.org/api.html

geoBoundaries documents gbOpen as CC BY 4.0 compliant and requires attribution. The importer stores each layer's source/license metadata when supplied; values vary by record, so review the metadata and attribution obligations before production use. No source dataset is bundled in this repository.

## Prerequisites

- Docker with Compose, Python 3.11+, network access to geoBoundaries, and PostgreSQL with PostGIS.
- Start the database using the repository's Compose configuration, then start the backend once to apply Flyway migrations:
  \`docker compose up -d\`
- If your compose file uses a different service or port, adapt \`DATABASE_URL\` accordingly.
- Install the importer dependencies in a virtual environment:

\`\`\`bash
python -m venv .venv
source .venv/bin/activate
pip install -r data-import/requirements.txt
\`\`\`

## Safe first run

Start with a one-layer trial and inspect the report before a full import:

\`\`\`bash
DATABASE_URL=postgresql://atlas:atlas@localhost:5432/atlas \\
  python data-import/import_geoboundaries.py --level ADM0 --limit 1
\`\`\`

Import all country outlines, then all ADM1 regions:

\`\`\`bash
DATABASE_URL=postgresql://atlas:atlas@localhost:5432/atlas \\
  python data-import/import_geoboundaries.py --level ADM0
DATABASE_URL=postgresql://atlas:atlas@localhost:5432/atlas \\
  python data-import/import_geoboundaries.py --level ADM1
\`\`\`

The importer downloads current source metadata at execution time; it does not claim a pinned upstream snapshot. Each source layer is processed transactionally. A failed layer rolls back without undoing successful layers. Re-running upserts imported rows; note that this pipeline does not delete old rows merely because a source layer stopped including them.

## Validation and coverage

Each country/region layer is checked for polygon geometry, empty geometry, validity, and SRID 4326 in PostGIS. Invalid source polygons are repaired with Shapely before import and again through PostGIS \`ST_MakeValid\`. All ADM0 features in a country layer are unioned, preserving islands/multipart country boundaries rather than keeping only the first feature. ISO-2 is resolved from the layer's feature properties or the ISO-3166 registry; an unknown ISO-3 is reported and skipped rather than guessed.

The script writes:
- timestamped JSON run report with per-level counts, failures, runtime and coverage;
- timestamped CSV with per-country ADM1 counts and validity;
- \`atlas_import_runs\` audit table in PostGIS with the run summary.

\`\`\`bash
# Inspect the existing database without downloading new layers
python data-import/import_geoboundaries.py --coverage-only

# Treat any country without an ADM1 row as a non-zero exit
python data-import/import_geoboundaries.py --coverage-only --fail-on-missing-adm1
\`\`\`

Reports default to \`data-import/reports/\`; override with \`--report-dir /path/to/reports\`. Avoid committing large or operational reports unless deliberately archiving a reviewed dataset release.

## Automated tests

\`\`\`bash
python -m pip install -r data-import/requirements.txt
cd data-import
python -m unittest -v
\`\`\`

Unit tests cover metadata response shapes, duplicate ISO-3 detection, geometry union/repair, non-polygon rejection, ISO mapping and metadata preservation. CI runs these tests and compiles the importer without needing live API access or a database.

## Attribution

Suggested dataset-level attribution:

> Administrative boundaries: geoBoundaries Database (www.geoboundaries.org). See per-layer source and license metadata stored with each imported record.

Before release, archive the generated report, review failed layers and missing ADM1 coverage, check the upstream terms for each record, and expose attribution in the app's About/Data Sources screen.
