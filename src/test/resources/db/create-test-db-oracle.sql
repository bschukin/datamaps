CALL DROP_TABLE_IF_EXIST('DDL_SCRIPT');
CREATE TABLE DDL_SCRIPT (
  id          NUMBER(10) NOT NULL  PRIMARY KEY,
  FILE_NAME   VARCHAR2(100),
  EXECUTED    timestamp
);
CALL drop_sequence_if_exist('DDL_SCRIPT_SEQ');
CALL create_sequence_if_not_exists2('DDL_SCRIPT_SEQ',1000);
CALL create_identity_if_not_exists('DDL_SCRIPT');

CALL DROP_TABLE_IF_EXIST('GAME');
CREATE TABLE GAME (
  id       VARCHAR2(30) PRIMARY KEY,
  name     VARCHAR2(30),
  metacriticId INTEGER
);

CALL DROP_TABLE_IF_EXIST('GAME_EPISODE');
CREATE TABLE GAME_EPISODE (
  id         VARCHAR2(30) PRIMARY KEY,
  name       VARCHAR2(30),
  game_id    VARCHAR2(30),
  FOREIGN KEY (game_id) REFERENCES GAME (id) ON DELETE CASCADE
);

CALL DROP_TABLE_IF_EXIST('GENDER');
CREATE TABLE GENDER (
  id         INTEGER PRIMARY KEY,
  name       VARCHAR2(30),
  IS_CLASSIC NUMBER(1,0)
);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (1, 'woman', 1);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (2, 'man', 1);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (3, 'foo', 0);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (4, 'bar', 0);

CALL DROP_TABLE_IF_EXIST('CITY');
CREATE TABLE CITY (
  id    INTEGER PRIMARY KEY,
  title VARCHAR2(30)
);

CALL DROP_TABLE_IF_EXIST('PERSON');
CREATE TABLE PERSON (
  id        NUMBER(10) PRIMARY KEY,
  name      VARCHAR2(30),
  email     VARCHAR2(50),
  gender_id NUMBER(10),
  last_name VARCHAR2(30),
  age       VARCHAR2(50),
  city_id   NUMBER(10),
  bio CLOB,
  photo BLOB,
  favorite_game_id VARCHAR2(30),
  FOREIGN KEY (gender_id) REFERENCES GENDER (id),
  FOREIGN KEY (city_id) REFERENCES CITY (id),
  FOREIGN KEY (favorite_game_id) REFERENCES GAME (id)
);
COMMENT ON TABLE PERSON IS 'We are the robots';
CALL DROP_SEQUENCE_IF_EXIST('PERSON_SEQ');
CALL create_sequence_if_not_exists2('PERSON_SEQ',1000);
CALL create_identity_if_not_exists('PERSON');


CALL DROP_TABLE_IF_EXIST('CHILD');
CREATE TABLE CHILD (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  PERSON_ID INTEGER,
  FOREIGN KEY (PERSON_ID) REFERENCES PERSON (id) ON DELETE CASCADE
);
CALL create_identity_if_not_exists('CHILD');

CALL DROP_TABLE_IF_EXIST('DEPARTMENT');
CREATE TABLE DEPARTMENT (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(50),
  parent_Id INTEGER,
  city_id   INTEGER,
  boss_id INTEGER,
  FOREIGN KEY (parent_Id) REFERENCES DEPARTMENT (id) ON DELETE CASCADE,
  FOREIGN KEY (boss_id) REFERENCES PERSON (id),
  FOREIGN KEY (city_id) REFERENCES CITY (id)
);
COMMENT ON COLUMN DEPARTMENT.name IS 'Name of deparment';
CALL create_identity_if_not_exists('DEPARTMENT');

CALL DROP_TABLE_IF_EXIST('WORKER_DEPARTMENT');
CREATE TABLE WORKER_DEPARTMENT (
  id            INTEGER PRIMARY KEY,
  WORKER_Id     INTEGER,
  DEPARTMENT_ID INTEGER,
  FOREIGN KEY (WORKER_Id) REFERENCES PERSON (id) ON DELETE CASCADE,
  FOREIGN KEY (DEPARTMENT_ID) REFERENCES DEPARTMENT (id) ON DELETE SET NULL
);

CALL DROP_TABLE_IF_EXIST('PROJECT');
CREATE TABLE PROJECT (
  id   INTEGER PRIMARY KEY,
  name VARCHAR(30)
);

CALL DROP_TABLE_IF_EXIST('TASK');
CREATE TABLE TASK (
  id         INTEGER PRIMARY KEY,
  name       VARCHAR(30),
  project_Id INTEGER,
  FOREIGN KEY (project_Id) REFERENCES Project (id) ON DELETE CASCADE
);
CALL create_identity_if_not_exists('TASK');

CALL DROP_TABLE_IF_EXIST('PROJECT_WORKER');
CREATE TABLE PROJECT_WORKER (
  id         INTEGER PRIMARY KEY,
  project_Id INTEGER,
  WORKER_Id  INTEGER,
  FOREIGN KEY (Project_Id) REFERENCES Project (id) ON DELETE CASCADE,
  FOREIGN KEY (WORKER_Id) REFERENCES PERSON (id)
);
CALL drop_sequence_if_exist('PROJECT_WORKER_SEQ');
CALL create_sequence_if_not_exists2('PROJECT_WORKER_SEQ',1000);
CALL create_identity_if_not_exists('PROJECT_WORKER');


CALL DROP_TABLE_IF_EXIST('CHECKLIST');
CREATE TABLE CHECKLIST (
  id      INTEGER PRIMARY KEY,
  name    VARCHAR(30),
  TASK_ID INTEGER,
  FOREIGN KEY (TASK_ID) REFERENCES TASK (id) ON DELETE CASCADE
);
CALL create_identity_if_not_exists('CHECKLIST');

CALL DROP_TABLE_IF_EXIST('FILM');
CREATE TABLE FILM (
  id      NUMBER(10) PRIMARY KEY,
  name    VARCHAR2(30),
  film_type VARCHAR2(30),
  film_type2 VARCHAR2(30)
);
CALL create_identity_if_not_exists('FILM');

--ALTER TABLE FILM ADD CONSTRAINT FILM_film_type CHECK ("film_type" IN ('Drama',  'Boevik','Unknown--Enum--Value') );
--ALTER TABLE FILM ADD CONSTRAINT FILM_film_type2 CHECK ("film_type2" IN ('Drama-2',  'Boevik-2') );


--/******************************************************
--/*****ТЕСТЫ НА DBDIFF
--/******************************************************
CALL DROP_TABLE_IF_EXIST('TBD01');
CALL DROP_TABLE_IF_EXIST('TBD0');
CALL DROP_TABLE_IF_EXIST('TBD01_CHILD');

CREATE TABLE TBD0 (
  id        INTEGER PRIMARY KEY
);

CREATE TABLE TBD01 (
  id        INTEGER PRIMARY KEY,
  name       VARCHAR2 (30),
  name2      VARCHAR2(30),
  someiny    INTEGER,
  tbd0_id INTEGER,
  FOREIGN KEY (tbd0_id) REFERENCES TBD0 (id) ON DELETE CASCADE,
  tbd01_id INTEGER,
  FOREIGN KEY (tbd01_id) REFERENCES TBD0 (id) ON DELETE CASCADE,
  oldName VARCHAR (30),
  booleanField NUMBER (1,0)

);
CREATE TABLE TBD01_CHILD (
  id         INTEGER PRIMARY KEY,
  tbd01_id INTEGER,
  FOREIGN KEY (tbd01_id) REFERENCES TBD01 (id) ON DELETE CASCADE,
  tbd011_id INTEGER,
  FOREIGN KEY (tbd011_id) REFERENCES TBD01 (id) ON DELETE CASCADE
);

--/******************************************************
--/*****REMAPPTING TESTS
--/******************************************************
CALL DROP_TABLE_IF_EXIST('PREF_ANIMAL');
CREATE TABLE PREF_ANIMAL (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

CALL DROP_TABLE_IF_EXIST('PREF_LEG');
CREATE TABLE PREF_LEG (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  ANIMAL_ID INTEGER,
  FOREIGN KEY (ANIMAL_ID) REFERENCES PREF_ANIMAL (id) ON DELETE CASCADE
);

CREATE or REPLACE VIEW PERSON_VIEW
  AS
    SELECT id, name FROM PERSON  p;

CREATE or REPLACE VIEW PERSON_VIEW2
  AS
    SELECT id, name FROM PERSON  p;

CALL DROP_TABLE_IF_EXIST('MONUMENT');
CREATE TABLE MONUMENT (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  PERSON_ID INTEGER
);

CALL DROP_TABLE_IF_EXIST('REMAP01');
CREATE TABLE REMAP01 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

CALL DROP_TABLE_IF_EXIST('REMAP01_CHILD');
CREATE TABLE REMAP01_CHILD (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  first_id INTEGER,
  second_id INTEGER,
  FOREIGN KEY (first_id) REFERENCES REMAP01 (id) ON DELETE CASCADE,
  FOREIGN KEY (second_id) REFERENCES REMAP01 (id) ON DELETE CASCADE
);

CALL DROP_TABLE_IF_EXIST('REMAP02');
CREATE TABLE REMAP02 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

CALL DROP_TABLE_IF_EXIST('REMAP02_CHILD');
CREATE TABLE REMAP02_CHILD (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  first_id INTEGER,
  second_id INTEGER,
  FOREIGN KEY (first_id) REFERENCES REMAP01 (id)
);

CALL DROP_TABLE_IF_EXIST('DATE_TEST');
CREATE TABLE DATE_TEST(
  id   INTEGER NOT NULL PRIMARY KEY,
  tdate DATE,
  timestamp TIMESTAMP
);
CALL create_identity_if_not_exists('DATE_TEST');

CALL DROP_TABLE_IF_EXIST('REMAP_STRING_TO_JSON');
CREATE TABLE REMAP_STRING_TO_JSON(
  id   INTEGER NOT NULL PRIMARY KEY,
  data VARCHAR2(2000)  NOT NULL
);
CALL create_identity_if_not_exists('REMAP_STRING_TO_JSON');

--/******************************************************
--/*****IMP
--/******************************************************
CALL DROP_TABLE_IF_EXIST('IMP04');
CREATE TABLE IMP04 (
  id        NUMBER(10) PRIMARY KEY,
  name      VARCHAR2(30)
);

CALL DROP_TABLE_IF_EXIST('IMP05');
CREATE TABLE IMP05 (
  id        NUMBER(10) PRIMARY KEY,
  name      VARCHAR2(30)
);

CALL DROP_TABLE_IF_EXIST('IMP01');
CREATE TABLE IMP01 (
  id        NUMBER(10) PRIMARY KEY,
  name      VARCHAR2(30),
  foo    VARCHAR2(3),
  bar    VARCHAR2(3),
  imp04_id NUMBER(10),
  imp05_id NUMBER(10),
  FOREIGN KEY (imp04_id) REFERENCES IMP04 (id),
  FOREIGN KEY (imp05_id) REFERENCES IMP05 (id)
);

CALL DROP_TABLE_IF_EXIST('IMP02');
CREATE TABLE IMP02 (
  id        NUMBER(10) PRIMARY KEY,
  name      VARCHAR2(30),
  imp01_id NUMBER(10),
  FOREIGN KEY (imp01_id) REFERENCES IMP01 (id) ON DELETE CASCADE
);

CALL DROP_TABLE_IF_EXIST('IMP03');
CREATE TABLE IMP03 (
  id        NUMBER(10) PRIMARY KEY,
  name      VARCHAR2(30),
  imp01_id NUMBER(10),
  FOREIGN KEY (imp01_id) REFERENCES IMP01 (id) ON DELETE CASCADE
);
--/******************************************************
--/*****ATTACH TESTS
--/******************************************************
CALL DROP_TABLE_IF_EXIST('ATTACH');
CREATE TABLE  ATTACH (
  id         NUMBER(10) PRIMARY KEY,
  name       VARCHAR2(30),
  data       CLOB,
  data2      CLOB
);
CALL create_identity_if_not_exists('ATTACH');

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


--CALL DROP_TABLE_IF_EXIST('TEMPORAL1');

--CREATE TABLE  TEMPORAL1 (
--  id         NUMBER(10) PRIMARY KEY,
--  name       VARCHAR2(30),
--  startDate    DATE default to_date(-9223372036832400000, 'YYYYMMDD'),
--  endDate     DATE default to_date(9223372036825200000, 'YYYYMMDD')
--);

--CALL create_sequence_if_not_exists('TEMPORAL1_SEQ');

--CALL create_identity_if_not_exists('TEMPORAL1');

--INSERT INTO TEMPORAL1 (name) VALUES ('test01');

CALL DROP_TABLE_IF_EXIST('UUID1');
CREATE TABLE  UUID1 (
  id         CHAR(36) PRIMARY KEY NOT NULL,
  name       VARCHAR2(30)
);

CALL DROP_TABLE_IF_EXIST('WEATHER');
CREATE TABLE  WEATHER (
  id              CHAR(36)  PRIMARY KEY NOT NULL,
  name            varchar2 (100) NOT NULL,
  startDate       DATE NOT NULL,
  endDate         DATE NOT NULL
);

CALL DROP_TABLE_IF_EXIST('DAY_WEATHER');
CREATE TABLE  DAY_WEATHER (
  id              CHAR(36)  PRIMARY KEY NOT NULL,
  name            varchar2 (100) NOT NULL,
  startDate       DATE NOT NULL,
  endDate         DATE NOT NULL,
  WEATHER_ID      CHAR(36)  NOT NULL,
  FOREIGN KEY (WEATHER_ID) REFERENCES WEATHER (id) ON DELETE CASCADE
);

CALL DROP_TABLE_IF_EXIST('PLANT_TYPE');
CREATE TABLE PLANT_TYPE (
  id        NUMBER(10) PRIMARY KEY  NOT NULL ,
  name      VARCHAR2(30) UNIQUE
);
CALL create_identity_if_not_exists('PLANT_TYPE');

CALL DROP_TABLE_IF_EXIST('PLANT');
CREATE TABLE PLANT (
  id        NUMBER(10) PRIMARY KEY  NOT NULL,
  name      VARCHAR2(30),
  type_id      VARCHAR2(30),
  FOREIGN KEY (type_id) REFERENCES PLANT_TYPE (name) ON DELETE CASCADE
);
CALL create_identity_if_not_exists('PLANT');

CALL DROP_TABLE_IF_EXIST('NATION');
CREATE TABLE NATION (
  id         CHAR(36) PRIMARY KEY  NOT NULL,
  record_Id  CHAR(36) NOT NULL,
  name      VARCHAR2(30),
  start_Date DATE ,
  end_Date DATE
);

CALL DROP_TABLE_IF_EXIST('CAPITAL');
CREATE TABLE CAPITAL (
  id         CHAR(36) PRIMARY KEY  NOT NULL,
  record_id  CHAR(36),
  name       VARCHAR2(30),
  start_date DATE NOT NULL,
  end_date   DATE NOT NULL,
  nation_record_id  CHAR(36) NOT NULL,
  FOREIGN KEY (nation_record_id) REFERENCES NATION (id) ON DELETE CASCADE
);

CALL DROP_TABLE_IF_EXIST('NATION_REGISTRY');
CREATE TABLE NATION_REGISTRY (
  id        CHAR(36) PRIMARY KEY  NOT NULL,
  record_id  CHAR(36) ,
  name      VARCHAR2(50),
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  nation_record_id  CHAR(36) NOT NULL,
  FOREIGN KEY (nation_record_id ) REFERENCES NATION (id) ON DELETE CASCADE
);

CALL CREATE_TABLE_IF_NOT_EXISTS('USERPOLICY', '(
  id INTEGER not null primary key,
	name varchar2(50) not null,
	sessionExpirationTime number(10),
	passwordExpirationDays number(10),
	passwordEnterCount number(10),
	passwordMinLength number(10)
)');

CALL create_identity_if_not_exists('USERPOLICY');

CALL CREATE_TABLE_IF_NOT_EXISTS('USERACCOUNT', '(
	id INTEGER not null  primary key,
	name varchar2(50) not null,
	fullName varchar2(255),
	email varchar2(255),
	password varchar2(255),
	blocked char(1),
	blockedReason varchar2(1024),
	passwordChangeDate date,
	passwordEnterCount number(10),
	userPolicyId INTEGER  references userpolicy
)');
CALL create_identity_if_not_exists('USERACCOUNT');

CALL DROP_TABLE_IF_EXIST('IGORSMAP');
CREATE TABLE  IGORSMAP (
  id         INTEGER PRIMARY KEY,
  upperName  VARCHAR(30),
  data   CLOB
);
CALL create_identity_if_not_exists('IGORSMAP');


--/******************************************************
--/*****STATEMACHINE TESTS
--/******************************************************
CALL DROP_TABLE_IF_EXIST('DOGOVOR');
CREATE TABLE DOGOVOR (
  ID        INTEGER PRIMARY KEY  NOT NULL ,
  STATE_MACHINE_INSTANCE_ID  INTEGER  NOT NULL,
  DESCRIPTION  VARCHAR2(50)
);
CALL create_identity_if_not_exists('DOGOVOR');

CALL DROP_TABLE_IF_EXIST('DOGOVOR2');
CREATE TABLE DOGOVOR2 (
  ID        INTEGER PRIMARY KEY  NOT NULL ,
  STATE_MACHINE_INSTANCE_ID  INTEGER  NOT NULL,
  STATE_ID  INTEGER  NOT NULL,
  DESCRIPTION  VARCHAR2(50)
);
CALL create_identity_if_not_exists('DOGOVOR2');

CALL DROP_TABLE_IF_EXIST('DOGOVOR3');
CREATE TABLE DOGOVOR3 (
  ID        INTEGER PRIMARY KEY  NOT NULL ,
  STATE_ID  INTEGER  NOT NULL,
  DESCRIPTION  VARCHAR2(50)
);
CALL create_identity_if_not_exists('DOGOVOR3');


CALL DROP_TABLE_IF_EXIST('GUESS_NUMBER');
CREATE TABLE GUESS_NUMBER (
  ID        INTEGER PRIMARY KEY  NOT NULL ,
  STATE_MACHINE_INSTANCE_ID  INTEGER  NOT NULL,
  TRYCOUNT  INTEGER
);
CALL create_identity_if_not_exists('GUESS_NUMBER');


CALL DROP_TABLE_IF_EXIST('FOO_BAR');
CREATE TABLE FOO_BAR (
  id         INTEGER PRIMARY KEY NOT NULL,
  name       VARCHAR2(30),
  value      INTEGER
);

CALL create_identity_if_not_exists('FOO_BAR');