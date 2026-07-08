# Database Migration Service

Migrates a Liferay customer database from a legacy database (Oracle, MySQL,
MariaDB, DB2, or SQL Server) to a separate target **PostgreSQL** database,
copying both the standard Liferay schema and any customer customizations.

## Why

Customers moving to Liferay PaaS frequently run customized databases: extra
columns added to standard Liferay tables and entirely custom tables created
without Service Builder. Schema-only converters drop those custom tables, and
data tools that assume a known Service Builder schema cannot see the extra
columns. This service is driven entirely by live JDBC metadata, so whatever
exists in the source — standard or custom — is migrated.

## How it works

The migration is driven by the `DatabaseMigrationManager` OSGi service and
triggered from **Control Panel > System > Database Migration**, a dedicated
portlet (`database-migration-web`) restricted to omniadmins. The portlet starts
the migration on a background thread and polls the shared `MigrationStatus`,
rendering a live progress bar and per-table row counts.

It needs the source and target JDBC connections; the target must be a separate,
empty PostgreSQL database.

1. **Connect.** Source and target data sources are built from the supplied
   JDBC URLs (`MigrationDataSourceFactory`, backed by the portal's
   `DataSourceFactoryUtil`).

1. **Create schema** (`SchemaCreator`). The target is expected to be empty, so
   every source table is created from the source's live column metadata —
   standard Liferay tables and customer customizations alike. Custom columns
   added to standard tables come across automatically because the whole source
   column set is cloned, rather than diffed against a pre-existing target
   table. Types are mapped to PostgreSQL by
   `MigrationUtil#toPostgreSQLColumnType`, and identifiers are normalized per
   the target's stored case (`MigrationUtil#normalizeName`) so lookups resolve
   on PostgreSQL's lower-cased schema.

1. **Copy data** (`TableCopier`). Every source table is copied row by row,
   converting each value from its source SQL type to the target SQL type.
   Binary data is written as `bytea`. Each row is inserted inside its own
   savepoint, so a single corrupt or incompatible row is rolled back and
   skipped rather than aborting the whole table — the migration continues to
   the next row. Successful rows are committed in batches.

1. **Create indexes** (`SchemaCreator#createIndexes`). Once the data is loaded,
   each source table's secondary indexes are read from JDBC metadata and
   recreated on the target with `CREATE [UNIQUE] INDEX`. The primary-key index
   is skipped because it is already created with the table. Indexes are built
   after the copy so index maintenance does not slow the bulk inserts. Each
   index keeps its source name, and a failure to create any single index (for
   example, a name collision on PostgreSQL's schema-wide index namespace) is
   logged and skipped rather than aborting the run.

The data-copy and type-conversion logic is adapted from Liferay's
`portal-tools-db-migration-importer` (`DBCopyTablesProcess`), extended to no
longer skip custom tables or abort on extra source columns.

## Error dashboard

Because a real customer database can hold corrupt or incompatible values, a
failed row does not stop the run. Each skipped row is captured as a
`MigrationError` (table, primary-key identifier, SQL state, and driver message)
and surfaced in the **Migration Errors** panel of the portlet once the run
finishes.

For every error the dashboard renders a **suggested fix query** — an `UPDATE`
and a `DELETE` scoped to the offending row by its primary key — that an
omniadmin can copy and run directly against the target database to repair the
data. The dashboard only suggests queries; it never executes them. When a table
has no primary key, the suggestion falls back to a template with a
`<condition>` placeholder to fill in.

## Notes and limitations

- The target PostgreSQL database **must be empty** — it should not be a booted
  Liferay instance. Every source table is created fresh; a table that already
  exists on the target will fail the run with a "table already exists" error.
  Start each migration from a clean, empty database.
- Tables are copied in metadata order. Databases with strict foreign-key
  constraints may need the constraints deferred or a load order adjustment.
- Primary keys and secondary indexes are recreated on the target; foreign
  keys and other constraints (checks, defaults, triggers) are not. The target
  reproduces the source's columns, primary keys, and indexes only.
- The relevant JDBC driver for the source database must be available on the
  application server.