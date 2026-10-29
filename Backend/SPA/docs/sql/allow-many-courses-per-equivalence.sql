DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT constraint_row.conname
        FROM pg_constraint constraint_row
        JOIN pg_class table_row ON table_row.oid = constraint_row.conrelid
        JOIN pg_namespace schema_row ON schema_row.oid = table_row.relnamespace
        WHERE schema_row.nspname = 'academico'
          AND table_row.relname = 'convalidacion'
          AND constraint_row.contype = 'u'
          AND pg_get_constraintdef(constraint_row.oid) = 'UNIQUE (curso_2023_id)'
    LOOP
        EXECUTE format(
            'ALTER TABLE academico.convalidacion DROP CONSTRAINT %I',
            constraint_name
        );
    END LOOP;
END
$$;
