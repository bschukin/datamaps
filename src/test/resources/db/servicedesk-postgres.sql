--//////////////////Временные зоны ///////////////////////////////////////
--////////////////////////////////////////////////////////////////////

CREATE SEQUENCE IF NOT EXISTS timezone_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "timezone"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('timezone_id_seq'),
  name character varying(100) NOT NULL
);

--//////////////////Ответственное Подразделение БФТ///////////////////////////////////////
--////////////////////////////////////////////////////////////////////

CREATE SEQUENCE IF NOT EXISTS bft_subdivision_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "bftsubdivision"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('bft_subdivision_id_seq'),
  name character varying(100) NOT NULL
);

--//////////////////Тип контракта ///////////////////////////////////////
--////////////////////////////////////////////////////////////////////

CREATE SEQUENCE IF NOT EXISTS contractType_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "contracttype"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('contractType_id_seq'),
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
ALTER TABLE organisation DROP CONSTRAINT  IF EXISTS   "timeZoneId_fk";
ALTER TABLE organisation ADD constraint "timeZoneId_fk" FOREIGN KEY ("timeZoneId") REFERENCES timezone(id);

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "loadedFromPU" BOOLEAN;
COMMENT ON COLUMN organisation."loadedFromPU" IS 'Загружено из ПУ';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "description" boolean;
COMMENT ON COLUMN organisation."description" IS 'Примечание';

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS "bftSubdivisionId" Integer REFERENCES bftsubdivision(id);
COMMENT ON COLUMN organisation."bftSubdivisionId" IS 'Id подразделения ДЭ';

--//////////////////Дочерняя организация//////////////////////////////
--////////////////////////////////////////////////////////////////////
CREATE SEQUENCE IF NOT EXISTS organisationtree_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "organisationtree"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('organisationtree_id_seq')
);

ALTER TABLE organisationtree ADD COLUMN IF NOT EXISTS "parentId" bigint;
COMMENT ON COLUMN organisationtree."parentId" IS 'Головная организация';
ALTER TABLE organisationtree ADD FOREIGN KEY ("parentId") REFERENCES organisation(id) on delete cascade;

ALTER TABLE organisationtree ADD COLUMN IF NOT EXISTS "childId" bigint;
COMMENT ON COLUMN organisationtree."childId" IS 'Дочерняя организация';
ALTER TABLE organisationtree ADD FOREIGN KEY ("childId") REFERENCES organisation(id)  on delete cascade;


--//////////////////USER///////////////////////////////////////
--////////////////////////////////////////////////////////////////////
CREATE SEQUENCE IF NOT EXISTS orguser_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "orguser"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('orguser_id_seq')
);


ALTER TABLE orguser ADD COLUMN IF NOT EXISTS "name" character varying(256);
COMMENT ON COLUMN orguser."name" IS 'Имя пользователя';

ALTER TABLE orguser ADD COLUMN IF NOT EXISTS "mobile" character varying(20);
COMMENT ON COLUMN orguser."mobile" IS 'Мобильный телефон';

ALTER TABLE orguser ADD COLUMN IF NOT EXISTS "workPhone" character varying(20);
COMMENT ON COLUMN orguser."workPhone" IS 'Рабочий телефон';

ALTER TABLE orguser ADD COLUMN IF NOT EXISTS "skype" character varying(20);
COMMENT ON COLUMN orguser."skype" IS 'Скайп';

ALTER TABLE orguser ADD COLUMN IF NOT EXISTS "icq" character varying(20);
COMMENT ON COLUMN orguser."icq" IS 'ICQ';

ALTER TABLE orguser ADD COLUMN IF NOT EXISTS "position" character varying(100);
COMMENT ON COLUMN orguser."position" IS 'Должность';

ALTER TABLE orguser ADD COLUMN IF NOT EXISTS "organisationId" Integer;
COMMENT ON COLUMN orguser."organisationId" IS 'Организация';
ALTER TABLE orguser ADD FOREIGN KEY ("organisationId") REFERENCES organisation(id);

--//////////////////Контракт///////////////////////////////////////
--////////////////////////////////////////////////////////////////////


CREATE SEQUENCE IF NOT EXISTS contract_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "contract"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('contract_id_seq')
);

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "organisationId" Integer;
COMMENT ON COLUMN contract."organisationId" IS 'Заказчик';
ALTER TABLE contract ADD FOREIGN KEY ("organisationId") REFERENCES organisation(id);

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "number" character varying(20);
COMMENT ON COLUMN contract."number" IS 'Номер контракта';

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "conclusionDate" date;
COMMENT ON COLUMN contract."conclusionDate" IS 'Дата заключения';

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "contractTypeId" Integer;
COMMENT ON COLUMN contract."contractTypeId" IS 'Тип контракта';
ALTER TABLE contract ADD FOREIGN KEY ("contractTypeId") REFERENCES contracttype(id);

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "numberPU" character varying(20);
COMMENT ON COLUMN contract."numberPU" IS 'Номер контракта в ПУ';

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "state" character varying(30);
COMMENT ON COLUMN contract."state" IS 'Статус';

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "projectPU" character varying(20);
COMMENT ON COLUMN contract."projectPU" IS 'Проект в ПУ';

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "bftSubdivisionId" Integer;
COMMENT ON COLUMN contract."bftSubdivisionId" IS 'Ответственное подразделение';
ALTER TABLE contract ADD FOREIGN KEY ("bftSubdivisionId") REFERENCES bftsubdivision(id);

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "startDate" date;
COMMENT ON COLUMN contract."startDate" IS 'Действует с';

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "finishDate" date;
COMMENT ON COLUMN contract."finishDate" IS 'Действует по';

ALTER TABLE contract ADD COLUMN IF NOT EXISTS "active" boolean;
COMMENT ON COLUMN contract."active" IS 'Действующий';


--//////////////////Получатели услуг по контракту/////////////////////
--////////////////////////////////////////////////////////////////////
CREATE SEQUENCE IF NOT EXISTS contract_org_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "contractorg"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('contract_org_id_seq')
);

ALTER TABLE "contractorg" ADD COLUMN IF NOT EXISTS "contractId" bigint;
COMMENT ON COLUMN "contractorg"."contractId" IS 'Контракт';

ALTER TABLE "contractorg" DROP CONSTRAINT  IF EXISTS   "contractorg_contractId_fk";
ALTER TABLE "contractorg" ADD constraint "contractorg_contractId_fk" FOREIGN KEY ("contractId") REFERENCES contract(id) on delete cascade;;

ALTER TABLE "contractorg" ADD COLUMN IF NOT EXISTS "organisationId" bigint;
COMMENT ON COLUMN "contractorg"."organisationId" IS 'Клиент';

ALTER TABLE "contractorg" DROP CONSTRAINT  IF EXISTS   "contractorg_organisationId_fk";
ALTER TABLE "contractorg" ADD constraint "contractorg_organisationId_fk" FOREIGN KEY ("organisationId") REFERENCES organisation(id) on delete cascade;


--//////////////////Продукт/////////////////////
--////////////////////////////////////////////////////////////////////
CREATE SEQUENCE IF NOT EXISTS product_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "product"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('product_id_seq')
);
ALTER TABLE "product" ADD COLUMN IF NOT EXISTS "name" character varying(50);
COMMENT ON COLUMN "product"."name" IS 'Наименование';

ALTER TABLE "product" ADD COLUMN IF NOT EXISTS "email" character varying(50);
COMMENT ON COLUMN "product"."email" IS 'Email входящий';


--//////////////////Модуль///////////////////////////////////////////
--////////////////////////////////////////////////////////////////////
CREATE SEQUENCE IF NOT EXISTS module_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "module"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('module_id_seq')
);
ALTER TABLE "module" ADD COLUMN IF NOT EXISTS "name" character varying(50);
COMMENT ON COLUMN "module"."name" IS 'Наименование';

ALTER TABLE "module" ADD COLUMN IF NOT EXISTS "notActive" boolean;
COMMENT ON COLUMN "module"."notActive" IS 'Снят с баланса';

ALTER TABLE "module" ADD COLUMN IF NOT EXISTS "productId" bigint;
COMMENT ON COLUMN "module"."productId" IS 'Продукт';

ALTER TABLE "module" DROP CONSTRAINT  IF EXISTS   "module_productId_fk";
ALTER TABLE "module" ADD constraint "module_productId_fk" FOREIGN KEY ("productId") REFERENCES product(id) on delete cascade;


--//////////////////Предмет контракта (Контракт и проданные в рамках него продукты  (и модули))////
--////////////////////////////////////////////////////////////////////
CREATE SEQUENCE IF NOT EXISTS contractproduct_id_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS "contractproduct"
(
  id bigint NOT NULL PRIMARY KEY DEFAULT NEXTVAL('contractproduct_id_seq')
);

ALTER TABLE "contractproduct" ADD COLUMN IF NOT EXISTS "contractId" bigint;
COMMENT ON COLUMN "contractproduct"."contractId" IS 'Контракт';

ALTER TABLE "contractproduct" DROP CONSTRAINT  IF EXISTS   "contractproduct_contractId_fk";
ALTER TABLE "contractproduct" ADD constraint "contractproduct_contractId_fk" FOREIGN KEY ("contractId") REFERENCES contract(id) on delete cascade;

ALTER TABLE "contractproduct" ADD COLUMN IF NOT EXISTS "productId" bigint;
COMMENT ON COLUMN "contractproduct"."productId" IS 'Продукт';

ALTER TABLE "contractproduct" DROP CONSTRAINT  IF EXISTS   "contractproduct_productId_fk";
ALTER TABLE "contractproduct" ADD constraint "contractproduct_productId_fk" FOREIGN KEY ("productId") REFERENCES product(id) on delete cascade;
