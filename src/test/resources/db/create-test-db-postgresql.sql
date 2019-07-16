
REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--DROP SEQUENCE IF EXISTS DDL_SCRIPT_id_seq;

CREATE SEQUENCE IF NOT EXISTS DDL_SCRIPT_id_seq
  START WITH 1000;

DROP TABLE IF EXISTS DDL_SCRIPT CASCADE;

CREATE TABLE DDL_SCRIPT (
  id          INT NOT NULL DEFAULT NEXTVAL('DDL_SCRIPT_id_seq') PRIMARY KEY,
  FILE_NAME     VARCHAR(100),
  EXECUTED  timestamp
);

DROP TABLE IF EXISTS GAME CASCADE;
CREATE TABLE GAME (
  id         VARCHAR(30) PRIMARY KEY,
  name     VARCHAR(30),
  metacriticId INTEGER
);

DROP TABLE IF EXISTS GAME_EPISODE CASCADE;
CREATE TABLE GAME_EPISODE (
  id         VARCHAR(30) PRIMARY KEY,
  name       VARCHAR(30),
  game_id    VARCHAR(30),
  FOREIGN KEY (game_id) REFERENCES GAME (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS GENDER CASCADE;

CREATE TABLE GENDER (
  id         INTEGER PRIMARY KEY,
  name     VARCHAR(30),
  IS_CLASSIC BOOLEAN
);

DROP TABLE IF EXISTS CITY CASCADE;

CREATE TABLE CITY (
  id    INTEGER PRIMARY KEY,
  title VARCHAR(30)
);

INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (1, 'woman', 'yes');
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (2, 'man', 'yes');
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (3, 'foo', 'no');
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (4, 'bar', 'no');


DROP TABLE IF EXISTS PERSON CASCADE;

DROP SEQUENCE IF EXISTS PERSON_SEQ;
CREATE SEQUENCE PERSON_SEQ
  START WITH 1000;

DROP TABLE IF EXISTS PERSON CASCADE;

CREATE TABLE PERSON (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  email     VARCHAR(50),
  gender_id INTEGER,
  last_name VARCHAR(30),
  age       VARCHAR(50),
  city_id   INTEGER,
  bio BYTEA,
  photo BYTEA,
  favorite_game_id VARCHAR(30),
  FOREIGN KEY (gender_id) REFERENCES GENDER (id),
  FOREIGN KEY (city_id) REFERENCES CITY (id),
  FOREIGN KEY (favorite_game_id) REFERENCES GAME (id)
);

DROP TABLE IF EXISTS CHILD CASCADE;

CREATE TABLE CHILD (
  id        BIGSERIAL PRIMARY KEY,
  name      VARCHAR(30),
  PERSON_ID INTEGER,
  FOREIGN KEY (PERSON_ID) REFERENCES PERSON (id) ON DELETE CASCADE
);

COMMENT ON TABLE PERSON IS 'We are the robots';


DROP TABLE IF EXISTS Department CASCADE;

DROP SEQUENCE IF EXISTS Department_id_seq;

CREATE SEQUENCE Department_id_seq
  START WITH 1000;

CREATE TABLE Department (
  id        INT NOT NULL DEFAULT NEXTVAL('Department_id_seq') PRIMARY KEY,
  name      VARCHAR(30),
  parent_Id INTEGER,
  city_id   INTEGER,
  boss_id   INTEGER,
  FOREIGN KEY (parent_Id) REFERENCES Department (id) ON DELETE CASCADE,
  FOREIGN KEY (boss_id) REFERENCES PERSON (id),
  FOREIGN KEY (city_id) REFERENCES CITY (id)
);
COMMENT ON COLUMN Department.name IS 'Name of deparment';

DROP TABLE IF EXISTS WORKER_Department CASCADE;


CREATE TABLE WORKER_Department (
  id            INTEGER PRIMARY KEY,
  WORKER_Id     INTEGER,
  department_Id INTEGER,
  FOREIGN KEY (WORKER_Id) REFERENCES PERSON (id) ON DELETE CASCADE,
  FOREIGN KEY (department_Id) REFERENCES department (id) ON DELETE SET NULL
);

DROP TABLE IF EXISTS PROJECT CASCADE;

CREATE TABLE PROJECT (
  id   INTEGER PRIMARY KEY,
  name VARCHAR(30)
);

DROP TABLE IF EXISTS TASK CASCADE;


DROP SEQUENCE IF EXISTS TASK_id_seq;
CREATE SEQUENCE TASK_id_seq
  START WITH 1000;

CREATE TABLE TASK (
  id         INT NOT NULL DEFAULT NEXTVAL('TASK_id_seq') PRIMARY KEY,
  name       VARCHAR(30),
  project_Id INTEGER,
  FOREIGN KEY (project_Id) REFERENCES Project (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS CHECKLIST CASCADE;

DROP SEQUENCE IF EXISTS CHECKLIST_id_seq;
CREATE SEQUENCE CHECKLIST_id_seq
  START WITH 1000;

CREATE TABLE CHECKLIST (
  id      INT NOT NULL DEFAULT NEXTVAL('CHECKLIST_id_seq') PRIMARY KEY,
  name    VARCHAR(30),
  TASK_ID INTEGER,
  FOREIGN KEY (TASK_ID) REFERENCES TASK (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS FILM CASCADE;
CREATE TABLE FILM (
  id      SERIAL PRIMARY KEY,
  name    VARCHAR(30),
  film_type VARCHAR(30),
  film_type2 VARCHAR(30)
);
ALTER TABLE FILM ADD CONSTRAINT FILM_film_type CHECK ("film_type" IN ('Drama',  'Boevik','Unknown--Enum--Value') );
ALTER TABLE FILM ADD CONSTRAINT FILM_film_type2 CHECK ("film_type2" IN ('Drama-2',  'Boevik-2') );



DROP SEQUENCE IF EXISTS PROJECT_WORKER_id_seq CASCADE;
CREATE SEQUENCE PROJECT_WORKER_id_seq
  START WITH 1000;
DROP TABLE IF EXISTS PROJECT_WORKER CASCADE;
CREATE TABLE PROJECT_WORKER (
  id         INT NOT NULL DEFAULT NEXTVAL('PROJECT_WORKER_id_seq') PRIMARY KEY,
  Project_Id INTEGER,
  worker_Id  INTEGER,
  FOREIGN KEY (Project_Id) REFERENCES Project (id) ON DELETE CASCADE,
  FOREIGN KEY (worker_Id) REFERENCES PERSON (id)
);


DROP TABLE IF EXISTS ATTACH CASCADE;
CREATE TABLE  ATTACH (
  id         SERIAL PRIMARY KEY,
  name       VARCHAR(30),
  data       JSONB,
  data2      JSONB
);

DROP TABLE IF EXISTS IgorsMap CASCADE;
CREATE TABLE  IgorsMap (
  id         SERIAL PRIMARY KEY,
  upperName       VARCHAR(30),
  data       JSONB
);

INSERT INTO ATTACH (name, data) VALUES ('test02',null);

INSERT INTO ATTACH (name, data) VALUES ('test01',
'{"name":"hello",
  "menu": {
  "id": "file",
  "value": "File",
  "popup": {
    "menuitem": [
      {"value": "New", "onclick": "CreateNewDoc()"},
      {"value": "Open", "onclick": "OpenDoc()"},
      {"value": "Close", "onclick": "CloseDoc()"}
    ]
  }
}}');

INSERT INTO ATTACH (name, data) VALUES ('test04',
'{"name":"hello",
  "value": 100500,
   "bool": true
 }');

INSERT INTO ATTACH (name, data2) VALUES ('test05',
'{"name":"hella",
  "part": {
  "foo": "unknown",
  "bar": "314"
 }
}');

INSERT INTO ATTACH (name, data) VALUES ('test06',
'{
  "name":"John",
  "age":30,
  "cars":[ "Ford", "BMW", "Fiat" ]
}');

INSERT INTO ATTACH (name, data) VALUES ('test07',
'{
  "name":"Ivan",
  "age":33,
  "cars":[ "Ziguli"]
}');



DROP TABLE IF EXISTS TEMPORAL1 ;
CREATE TABLE  TEMPORAL1 (
  id         SERIAL PRIMARY KEY,
  name       VARCHAR(30),
  startDate    DATE default '-infinity',
  endDate     DATE default 'infinity'
);
INSERT INTO TEMPORAL1 (name) VALUES ('test01');


DROP TABLE IF EXISTS UUID1 ;
CREATE TABLE  UUID1 (
  id         UUID PRIMARY KEY NOT NULL,
  name       VARCHAR(30)
);

DROP TABLE IF EXISTS WEATHER CASCADE ;
CREATE TABLE  WEATHER (
  id              UUID  PRIMARY KEY NOT NULL,
  name            varchar (36) NOT NULL,
  startDate       DATE NOT NULL,
  endDate         DATE NOT NULL
);

DROP TABLE IF EXISTS DAY_WEATHER CASCADE ;
CREATE TABLE  DAY_WEATHER (
  id              UUID  PRIMARY KEY NOT NULL,
  name            varchar (36) NOT NULL,
  startDate       DATE NOT NULL,
  endDate         DATE NOT NULL,
  WEATHER_ID      UUID  NOT NULL,
  FOREIGN KEY (WEATHER_ID) REFERENCES WEATHER (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS PLANT_TYPE CASCADE;
CREATE TABLE PLANT_TYPE (
  id        SERIAL PRIMARY KEY  NOT NULL ,
  name      VARCHAR(30) UNIQUE
);

DROP TABLE IF EXISTS PLANT CASCADE;
CREATE TABLE PLANT (
  id        SERIAL PRIMARY KEY  NOT NULL,
  name      VARCHAR(30),
  type_id      VARCHAR(30),
  FOREIGN KEY (type_id) REFERENCES PLANT_TYPE (name) ON DELETE CASCADE
);


DROP TABLE IF EXISTS NATION CASCADE;
CREATE TABLE NATION (
  id         UUID PRIMARY KEY  NOT NULL,
  record_Id  UUID NOT NULL,
  name      VARCHAR(30),
  start_Date DATE ,
  end_Date DATE
);

DROP TABLE IF EXISTS CAPITAL CASCADE;
CREATE TABLE CAPITAL (
  id         UUID PRIMARY KEY  NOT NULL,
  record_id  UUID,
  name       VARCHAR(30),
  start_date DATE NOT NULL,
  end_date   DATE NOT NULL,
  nation_record_id  UUID NOT NULL,
  FOREIGN KEY (nation_record_id) REFERENCES NATION (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS NATION_REGISTRY CASCADE;
CREATE TABLE NATION_REGISTRY (
  id        UUID PRIMARY KEY  NOT NULL,
  record_id  UUID ,
  name      VARCHAR(30),
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  nation_record_id  UUID NOT NULL,
  FOREIGN KEY (nation_record_id ) REFERENCES NATION (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS FOO_BAR CASCADE;
CREATE TABLE FOO_BAR (
  id         SERIAL PRIMARY KEY NOT NULL,
  name       VARCHAR(30),
  value      int
);

--/******************************************************
--/*****ТЕСТЫ НА DBDIFF
--/******************************************************
DROP TABLE IF EXISTS TBD01 CASCADE;
DROP TABLE IF EXISTS TBD0 CASCADE;
DROP TABLE IF EXISTS TBD01_CHILD CASCADE;
DROP TABLE IF EXISTS TBD01_BABE CASCADE;


CREATE TABLE TBD0 (
  id        INTEGER PRIMARY KEY
);

CREATE TABLE TBD01 (
  id        bigint PRIMARY KEY,
  name       character VARYING (30),
  name2      character VARYING(30),
  someiny    INTEGER,
  tbd0_id INTEGER,
  FOREIGN KEY (tbd0_id) REFERENCES TBD0 (id) ON DELETE CASCADE,
  tbd01_id INTEGER,
  FOREIGN KEY (tbd01_id) REFERENCES TBD0 (id) ON DELETE CASCADE,
  oldName VARCHAR (30),
  booleanField BOOLEAN

);
CREATE TABLE TBD01_CHILD (
  id         INTEGER PRIMARY KEY,
  tbd01_id INTEGER,
  FOREIGN KEY (tbd01_id) REFERENCES TBD01 (id) ON DELETE CASCADE,
  tbd011_id INTEGER,
  FOREIGN KEY (tbd011_id) REFERENCES TBD01 (id) ON DELETE CASCADE
);

CREATE TABLE TBD01_BABE (
  id         INTEGER PRIMARY KEY,
  name       character VARYING (30)
);

--/******************************************************
--/*****REMAPPTING TESTS
--/******************************************************

DROP TABLE IF EXISTS PREF_ANIMAL CASCADE;
CREATE TABLE PREF_ANIMAL (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)

);

DROP TABLE IF EXISTS PREF_LEG CASCADE;

CREATE TABLE PREF_LEG (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  ANIMAL_ID INTEGER,
  FOREIGN KEY (ANIMAL_ID) REFERENCES PREF_ANIMAL (id) ON DELETE CASCADE
);


DROP VIEW IF EXISTS PERSON_VIEW;
CREATE or REPLACE VIEW PERSON_VIEW
  AS
    SELECT id, name FROM PERSON  p;

DROP VIEW IF EXISTS PERSON_VIEW2;
CREATE or REPLACE VIEW PERSON_VIEW2
  AS
    SELECT id, name FROM PERSON  p;

DROP TABLE IF EXISTS MONUMENT CASCADE;
CREATE TABLE MONUMENT (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  PERSON_ID INTEGER
);


DROP TABLE IF EXISTS REMAP01 CASCADE;
CREATE TABLE REMAP01 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

DROP TABLE IF EXISTS REMAP01_CHILD CASCADE;
CREATE TABLE REMAP01_CHILD (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  first_id INTEGER,
  second_id INTEGER,
  FOREIGN KEY (first_id) REFERENCES REMAP01 (id) ON DELETE CASCADE,
  FOREIGN KEY (second_id) REFERENCES REMAP01 (id) ON DELETE CASCADE
);


DROP TABLE IF EXISTS REMAP02 CASCADE;
CREATE TABLE REMAP02 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

DROP TABLE IF EXISTS REMAP02_CHILD CASCADE;
CREATE TABLE REMAP02_CHILD (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  first_id INTEGER,
  second_id INTEGER,
  FOREIGN KEY (first_id) REFERENCES REMAP01 (id)
);

DROP TABLE IF EXISTS DATE_TEST CASCADE;
CREATE TABLE IF NOT EXISTS DATE_TEST(
  id          SERIAL NOT NULL PRIMARY KEY,
  tdate date,
  timestamp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS REMAP_STRING_TO_JSON(
  id        SERIAL   NOT NULL PRIMARY KEY,
  data VARCHAR(2000)  NOT NULL
);



DROP TABLE IF EXISTS IMP04 CASCADE;
CREATE TABLE IMP04 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

DROP TABLE IF EXISTS IMP05 CASCADE;
CREATE TABLE IMP05 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

DROP TABLE IF EXISTS IMP01 CASCADE;
CREATE TABLE IMP01 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  foo    VARCHAR(3),
  bar    VARCHAR(3),
  imp04_id INTEGER,
  imp05_id INTEGER,
  FOREIGN KEY (imp04_id) REFERENCES IMP04 (id),
  FOREIGN KEY (imp05_id) REFERENCES IMP05 (id)
);

DROP TABLE IF EXISTS IMP02 CASCADE;
CREATE TABLE IMP02 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  imp01_id INTEGER,
  FOREIGN KEY (imp01_id) REFERENCES IMP01 (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS IMP03 CASCADE;
CREATE TABLE IMP03 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  imp01_id INTEGER,
  FOREIGN KEY (imp01_id) REFERENCES IMP01 (id) ON DELETE CASCADE
);

--/******************************************************
--/*****STATEMACHINE TESTS
--/******************************************************

DROP TABLE IF EXISTS DOGOVOR CASCADE;
CREATE TABLE DOGOVOR (
  id        SERIAL PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  description  VARCHAR(50)
);


DROP TABLE IF EXISTS DOGOVOR2 CASCADE;
CREATE TABLE DOGOVOR2 (
  id        SERIAL PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  state_id      INTEGER  NOT NULL,
  description  VARCHAR(50)
);


DROP TABLE IF EXISTS DOGOVOR3 CASCADE;
CREATE TABLE DOGOVOR3 (
  id        SERIAL PRIMARY KEY  NOT NULL ,
  state_id      INTEGER  NOT NULL,
  description  VARCHAR(50)
);

DROP TABLE IF EXISTS GUESS_NUMBER CASCADE;
CREATE TABLE GUESS_NUMBER (
  id        SERIAL PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  tryCount  INTEGER
);


--/******************************************************
--/*****RLS TESTS
--/******************************************************

DROP OPERATOR CLASS IF EXISTS _uuid_ops USING gin CASCADE;
CREATE OPERATOR CLASS _uuid_ops DEFAULT
  FOR TYPE _uuid USING gin AS
  OPERATOR 1 &&(anyarray, anyarray),
  OPERATOR 2 @>(anyarray, anyarray),
  OPERATOR 3 <@(anyarray, anyarray),
  OPERATOR 4 =(anyarray, anyarray),
  FUNCTION 1 uuid_cmp(uuid, uuid),
  FUNCTION 2 ginarrayextract(anyarray, internal, internal),
  FUNCTION 3 ginqueryarrayextract(anyarray, internal, smallint, internal, internal, internal, internal),
  FUNCTION 4 ginarrayconsistent(internal, smallint, anyarray, integer, internal, internal, internal, internal),
  STORAGE uuid;

CREATE OR REPLACE FUNCTION ice_readtokens() RETURNS uuid[] AS $$ SELECT string_to_array(current_setting('user.read-tokens'), ',')::uuid[] $$ language sql;

CREATE OR REPLACE FUNCTION ice_writetokens() RETURNS uuid[] AS $$ SELECT string_to_array(current_setting('user.write-tokens'), ',')::uuid[] $$ language sql;

create table if not exists test_org (
	id bigserial primary key,
	name text not null ,
	parent_id bigint,
	canBeOwner boolean,
	mainToken text,
	allowReadChildrenObject boolean,
	allowWriteChildrenObject boolean,
	FOREIGN KEY (parent_Id) REFERENCES test_org (id)
);

create table if not exists test_org_user (
	id bigserial primary key,
	user_account_id bigint not null,
	test_org_id bigint,
	--FOREIGN KEY (user_account_id) REFERENCES user_account (id),
	FOREIGN KEY (test_org_id) REFERENCES test_org (id)
);

drop table if exists test_rls_op;
create table  if not exists test_rls_op (
  id bigserial PRIMARY KEY,
  displayname text NOT NULL
);

ALTER TABLE IF EXISTS test_rls_op ADD COLUMN IF NOT EXISTS readaccess uuid[];
ALTER TABLE IF EXISTS test_rls_op ADD COLUMN IF NOT EXISTS writeaccess uuid[];
ALTER TABLE IF EXISTS test_rls_op ADD COLUMN IF NOT EXISTS objowner uuid[];

CREATE INDEX IF NOT EXISTS test_rls_op_readaccess_idx ON test_rls_op USING GIN ((array_cat(readaccess, objowner)));
CREATE INDEX IF NOT EXISTS test_rls_op_writeaccess_idx ON test_rls_op USING GIN ((array_cat(writeaccess, objowner)));

DROP POLICY IF EXISTS test_rls_op_policy_select ON test_rls_op;
CREATE POLICY test_rls_op_policy_select ON test_rls_op FOR SELECT USING (array_cat(readaccess, objowner) && ice_readtokens());

DROP POLICY IF EXISTS test_rls_op_policy_update ON test_rls_op;
CREATE POLICY test_rls_op_policy_update ON test_rls_op FOR UPDATE USING (array_cat(writeaccess, objowner) && ice_writetokens());

DROP POLICY IF EXISTS test_rls_op_policy_delete ON test_rls_op;
CREATE POLICY test_rls_op_policy_delete ON test_rls_op FOR DELETE USING (array_cat(writeaccess, objowner) && ice_writetokens());

DROP POLICY IF EXISTS test_rls_op_policy_insert ON test_rls_op;
CREATE POLICY test_rls_op_policy_insert ON test_rls_op FOR INSERT WITH CHECK (array_cat(writeaccess, objowner) && ice_writetokens());

ALTER TABLE test_rls_op ENABLE ROW LEVEL SECURITY;


DROP TABLE IF EXISTS HAND CASCADE;
CREATE TABLE HAND (
  id       INT PRIMARY KEY,
  name     VARCHAR(30) UNIQUE,
  name2     VARCHAR(30),
  name3     VARCHAR(30)
);
ALTER TABLE HAND ADD CONSTRAINT constraint_xxx UNIQUE (name2, name3);


DROP TABLE IF EXISTS FINGER CASCADE;
CREATE TABLE FINGER (
  id         INT PRIMARY KEY,
  name       VARCHAR(30),
  hand_id    INT NOT NULL,
  FOREIGN KEY (hand_id) REFERENCES HAND (id)
);


DROP TABLE IF EXISTS HAND2 CASCADE;
CREATE TABLE HAND2 (
  id       INT PRIMARY KEY,
  name     VARCHAR(30)
);


DROP TABLE IF EXISTS HAND3 CASCADE;
CREATE TABLE HAND3 (
  id       INT PRIMARY KEY,
  name     VARCHAR(30)
);
COMMENT ON TABLE HAND3 IS 'I am tha hand';

INSERT INTO HAND(id, name) VALUES (1, 'left');
INSERT INTO FINGER(id, name, hand_id) VALUES (1, 'noname', 1);