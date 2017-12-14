--//////////////////Временные зоны ///////////////////////////////////////
--////////////////////////////////////////////////////////////////////

CREATE SEQUENCE IF NOT EXISTS timezone_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "timezone"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('timezone_id_seq'),
  name character varying(100) NOT NULL
);

--//////////////////Подразделение ПУ///////////////////////////////////////
--////////////////////////////////////////////////////////////////////

CREATE SEQUENCE IF NOT EXISTS subdivision_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "subdivision"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('subdivision_id_seq'),
  name character varying(100) NOT NULL
);


--//////////////////ОРГАНИЗАЦИЯ///////////////////////////////////////
--////////////////////////////////////////////////////////////////////

CREATE SEQUENCE IF NOT EXISTS organisation_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "organisation"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('organisation_id_seq'),
  name character varying(100) NOT NULL
);

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "fullName" character varying(256);
COMMENT ON COLUMN organisation ."fullName" IS 'Полное наименование';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "legalAddress" character varying(256);
COMMENT ON COLUMN organisation."legalAddress" IS 'Юридический aдрес';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "actualAddress" character varying(256);
COMMENT ON COLUMN organisation."actualAddress" IS 'Фактический адрес';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "INN" character(12);
COMMENT ON COLUMN organisation."INN" IS 'ИНН';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "KPP" character(9);
COMMENT ON COLUMN organisation."KPP" IS 'КПП';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "FIAS" character varying(512);
COMMENT ON COLUMN organisation."FIAS" IS 'ФИАС';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "telephone" character varying(12);
COMMENT ON COLUMN organisation."telephone" IS 'Телефон';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "email" character varying(100);
COMMENT ON COLUMN organisation."email" IS 'Электронный ящик';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "isActual" BOOLEAN;
COMMENT ON COLUMN organisation."isActual" IS 'Актуален';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "VIP" BOOLEAN;
COMMENT ON COLUMN organisation."VIP" IS 'VIP';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "workTimeStart" Integer;
COMMENT ON COLUMN organisation."workTimeStart" IS 'Работа с';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "workTimeEnd" Integer;
COMMENT ON COLUMN organisation."workTimeEnd" IS 'Работа по';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "dinnerTimeStart" Integer;
COMMENT ON COLUMN organisation."dinnerTimeStart" IS 'Обед с';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "dinnerTimeEnd" Integer;
COMMENT ON COLUMN organisation."dinnerTimeEnd" IS 'Обед по';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "timeZoneId" Integer;
COMMENT ON COLUMN organisation."timeZoneId" IS 'Часовой пояс';
ALTER TABLE organisation ADD FOREIGN KEY ("timeZoneId") REFERENCES timezone(id);

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "loadedFromPU" BOOLEAN;
COMMENT ON COLUMN organisation."loadedFromPU" IS 'Загружено из ПУ';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "description" boolean;
COMMENT ON COLUMN organisation."description" IS 'Примечание';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "subdivisionId" Integer;
COMMENT ON COLUMN organisation."subdivisionId" IS 'Id подразделения ДЭ';
ALTER TABLE organisation ADD FOREIGN KEY ("subdivisionId") REFERENCES subdivision(id)
