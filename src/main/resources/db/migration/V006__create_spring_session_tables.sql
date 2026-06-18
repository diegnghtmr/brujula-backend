-- V006: Create Spring Session JDBC tables in platform schema
--
-- Module: sharedkernel (platform schema)
-- Schema: platform
-- Satisfies: Session Persistence spec
-- Note: spring.session.jdbc.initialize-schema=never — Flyway is the sole DDL source.
--       spring.session.jdbc.table-name=platform.SPRING_SESSION for schema-prefixed access.
--
-- Source: canonical Spring Session JDBC DDL for PostgreSQL (spring-session-jdbc 4.0.2
--         org/springframework/session/jdbc/schema-postgresql.sql) adapted to use
--         the 'platform' schema prefix.

CREATE TABLE IF NOT EXISTS platform."SPRING_SESSION" (
    "PRIMARY_ID"          CHAR(36)        NOT NULL,
    "SESSION_ID"          CHAR(36)        NOT NULL,
    "CREATION_TIME"       BIGINT          NOT NULL,
    "LAST_ACCESS_TIME"    BIGINT          NOT NULL,
    "MAX_INACTIVE_INTERVAL" INT           NOT NULL,
    "EXPIRY_TIME"         BIGINT          NOT NULL,
    "PRINCIPAL_NAME"      VARCHAR(100),

    CONSTRAINT "SPRING_SESSION_PK" PRIMARY KEY ("PRIMARY_ID")
);

CREATE UNIQUE INDEX IF NOT EXISTS "SPRING_SESSION_IX1"
    ON platform."SPRING_SESSION" ("SESSION_ID");

CREATE INDEX IF NOT EXISTS "SPRING_SESSION_IX2"
    ON platform."SPRING_SESSION" ("EXPIRY_TIME");

CREATE INDEX IF NOT EXISTS "SPRING_SESSION_IX3"
    ON platform."SPRING_SESSION" ("PRINCIPAL_NAME");

CREATE TABLE IF NOT EXISTS platform."SPRING_SESSION_ATTRIBUTES" (
    "SESSION_PRIMARY_ID"  CHAR(36)        NOT NULL,
    "ATTRIBUTE_NAME"      VARCHAR(200)    NOT NULL,
    "ATTRIBUTE_BYTES"     BYTEA           NOT NULL,

    CONSTRAINT "SPRING_SESSION_ATTRIBUTES_PK"
        PRIMARY KEY ("SESSION_PRIMARY_ID", "ATTRIBUTE_NAME"),
    CONSTRAINT "SPRING_SESSION_ATTRIBUTES_FK"
        FOREIGN KEY ("SESSION_PRIMARY_ID")
        REFERENCES platform."SPRING_SESSION" ("PRIMARY_ID")
        ON DELETE CASCADE
);

COMMENT ON TABLE platform."SPRING_SESSION"
    IS 'Spring Session JDBC session store in the platform schema. Created by Flyway (initialize-schema=never).';
COMMENT ON TABLE platform."SPRING_SESSION_ATTRIBUTES"
    IS 'Spring Session JDBC session attributes store. Created by Flyway.';
