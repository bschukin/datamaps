
--////////////////////////////////////////////////////////////////////
--//////////////////ОРГАНИЗАЦИЯ///////////////////////////////////////
--////////////////////////////////////////////////////////////////////

CREATE SEQUENCE IF NOT EXISTS organisation_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "Organisation"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('organisation_id_seq'),
  name character varying(100) NOT NULL
);

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "fullName" character varying(256);
COMMENT ON COLUMN Organisation ."fullName" IS 'Полное наименование';

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "legalAddress" character varying(256);
COMMENT ON COLUMN Organisation."legalAddress" IS 'Юридический aдрес';

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "actualAddress" character varying(256);
COMMENT ON COLUMN Organisation."actualAddress" IS 'Фактический адрес';

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "INN" character(12);
COMMENT ON COLUMN Organisation."INN" IS 'ИНН';

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "KPP" character(9);
COMMENT ON COLUMN Organisation."KPP" IS 'КПП';

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "FIAS" character varying(512);
COMMENT ON COLUMN Organisation."FIAS" IS 'ФИАС';

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "telephone" character varying(12);
COMMENT ON COLUMN Organisation."telephone" IS 'Телефон';

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "email" character varying(100);
COMMENT ON COLUMN Organisation."email" IS 'Электронный ящик';

ALTER TABLE Organisation ADD COLUMN IF NOT EXISTS "isActual" BOOLEAN;
