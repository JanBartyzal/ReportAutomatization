-- Grant engine-data read access to ingestor.files for cross-schema dashboard SQL queries.
-- Dashboard Custom SQL widgets need to JOIN data.parsed_tables with ingestor.files
-- to resolve file names (e.g. WHERE f.filename = 'Test.xlsx').

GRANT USAGE ON SCHEMA ingestor TO engine_data_user;
GRANT SELECT ON ingestor.files TO engine_data_user;
