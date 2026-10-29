ALTER TABLE academico.seccion
    ADD COLUMN IF NOT EXISTS cupos_plan_2018 integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cupos_plan_2023 integer NOT NULL DEFAULT 0;

ALTER TABLE academico.seccion
    ALTER COLUMN capacidad SET DEFAULT 35;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'academico.seccion'::regclass
          AND conname = 'seccion_cupos_no_superan_capacidad'
    ) THEN
        ALTER TABLE academico.seccion
            ADD CONSTRAINT seccion_cupos_no_superan_capacidad
            CHECK (
                cupos_plan_2018 >= 0
                AND cupos_plan_2023 >= 0
                AND cupos_plan_2018 + cupos_plan_2023 <= capacidad
            );
    END IF;
END
$$;
