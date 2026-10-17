# Global geographic data import

This importer downloads country boundaries (ADM0) and first-level administrative boundaries (ADM1) from the geoBoundaries **gbOpen** API and writes them to PostgreSQL/PostGIS.

Source documentation: https://www.geoboundaries.org/api.html

geoBoundaries states that gbOpen is CC BY 4.0 compliant and requires attribution. The importer stores source, license, version/build date and reference date per layer where available. Individual source licenses and metadata vary; review the metadata and satisfy attribution obligations before production use.

## Run

1. Start PostGIS from the repository root: `docker compose up -d`.
2. Run Flyway migrations by starting the backend once, or apply the SQL migration to the database.
3. Create an isolated Python environment and install dependencies:

   ```bash
   python -m venv .venv
   source .venv/bin/activate
   pip install -r data-import/requirements.txt
   ```

4. Trial one country layer first:

   ```bash
   DATABASE_URL=postgresql://atlas:atlas@localhost:5432/atlas python data-import/import_geoboundaries.py --level ADM0 --limit 1
   ```

5. Import country outlines, then first-level regions:

   ```bash
   DATABASE_URL=postgresql://atlas:atlas@localhost:5432/atlas python data-import/import_geoboundaries.py --level ADM0
   DATABASE_URL=postgresql://atlas:atlas@localhost:5432/atlas python data-import/import_geoboundaries.py --level ADM1
   ```

The importer does not invent missing ISO-2 codes. A layer missing a reviewed ISO-2 value is reported and skipped. Review the final failure report and metadata coverage before describing the dataset as complete. Use `--limit` for controlled test runs; network availability and source metadata can vary.
