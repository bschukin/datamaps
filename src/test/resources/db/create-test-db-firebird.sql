/*CREATE SEQUENCE IF NOT EXISTS DDL_SCRIPT_id_seq
  START WITH 1000;

DROP TABLE IF EXISTS DDL_SCRIPT CASCADE;
*/

CREATE SEQUENCE DDL_SCRIPT_SEQ;

CREATE TABLE DDL_SCRIPT (
  id            INTEGER   PRIMARY KEY,
  FILE_NAME     VARCHAR(100),
  EXECUTED      timestamp
);


CREATE TABLE GAME (
  id         VARCHAR(30) PRIMARY KEY,
  name     VARCHAR(30),
  metacriticId INTEGER
);


CREATE TABLE GAME_EPISODE (
  id         VARCHAR(30) PRIMARY KEY,
  name       VARCHAR(30),
  game_id    VARCHAR(30),
  FOREIGN KEY (game_id) REFERENCES GAME (id) ON DELETE CASCADE
);


CREATE TABLE GENDER (
  id         INTEGER PRIMARY KEY,
  name     VARCHAR(30),
  IS_CLASSIC BOOLEAN
);

CREATE SEQUENCE CITY_SEQ;
ALTER SEQUENCE CITY_SEQ RESTART WITH 1000;
CREATE TABLE CITY (
  id    INTEGER  PRIMARY KEY,
  title VARCHAR(30)
);

INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (1, 'woman', 1);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (2, 'man', 1);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (3, 'foo', 0);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (4, 'bar', 0);


CREATE SEQUENCE PERSON_SEQ;
ALTER SEQUENCE PERSON_SEQ RESTART WITH 1000;

CREATE TABLE PERSON (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  email     VARCHAR(50),
  gender_id INTEGER,
  last_name VARCHAR(30),
  age       VARCHAR(50),
  city_id   INTEGER,
  bio BLOB,
  photo BLOB,
  favorite_game_id VARCHAR(30),
  FOREIGN KEY (gender_id) REFERENCES GENDER (id),
  FOREIGN KEY (city_id) REFERENCES CITY (id),
  FOREIGN KEY (favorite_game_id) REFERENCES GAME (id)
);
COMMENT ON TABLE PERSON IS 'We are the robots';

CREATE SEQUENCE CHILD_SEQ;
CREATE TABLE CHILD (
  id        bigint  PRIMARY KEY,
  name      VARCHAR(30),
  PERSON_ID INTEGER,
  FOREIGN KEY (PERSON_ID) REFERENCES PERSON (id) ON DELETE CASCADE
);

COMMENT ON TABLE PERSON IS 'We are the robots';


CREATE SEQUENCE Department_SEQ;
ALTER SEQUENCE Department_SEQ RESTART WITH 1000;

CREATE TABLE Department (
  id        INT  NOT NULL  PRIMARY KEY,
  name      VARCHAR(30),
  parent_Id INTEGER,
  city_id   INTEGER,
  boss_id   INTEGER,
  FOREIGN KEY (parent_Id) REFERENCES Department (id) ON DELETE CASCADE,
  FOREIGN KEY (boss_id) REFERENCES PERSON (id),
  FOREIGN KEY (city_id) REFERENCES CITY (id)
);
COMMENT ON COLUMN Department.name IS 'Name of deparment';



CREATE TABLE WORKER_Department (
  id            INTEGER PRIMARY KEY,
  WORKER_Id     INTEGER,
  department_Id INTEGER,
  FOREIGN KEY (WORKER_Id) REFERENCES PERSON (id) ON DELETE CASCADE,
  FOREIGN KEY (department_Id) REFERENCES department (id) ON DELETE SET NULL
);


CREATE TABLE PROJECT (
  id   INTEGER PRIMARY KEY,
  name VARCHAR(30)
);

CREATE SEQUENCE TASK_SEQ;
ALTER SEQUENCE TASK_SEQ RESTART WITH 1000;

CREATE TABLE TASK (
  id         INT    NOT NULL   PRIMARY KEY,
  name       VARCHAR(30),
  project_Id INTEGER,
  FOREIGN KEY (project_Id) REFERENCES Project (id) ON DELETE CASCADE
);


CREATE SEQUENCE CHECKLIST_SEQ;
ALTER SEQUENCE CHECKLIST_SEQ RESTART WITH 1000;

CREATE TABLE CHECKLIST (
  id      INT   PRIMARY KEY  NOT NULL,
  name    VARCHAR(30),
  TASK_ID INTEGER,
  FOREIGN KEY (TASK_ID) REFERENCES TASK (id) ON DELETE CASCADE
);

CREATE SEQUENCE FILM_SEQ;
CREATE TABLE FILM (
  id      INT  NOT NULL   PRIMARY KEY,
  name    VARCHAR(30),
  film_type VARCHAR(30),
  film_type2 VARCHAR(30)
);

ALTER TABLE FILM ADD CONSTRAINT FILM_film_type CHECK ("FILM_TYPE" IN ('Drama',  'Boevik','Unknown--Enum--Value') );
ALTER TABLE FILM ADD CONSTRAINT FILM_film_type2 CHECK ("FILM_TYPE2" IN ('Drama-2',  'Boevik-2') );

CREATE SEQUENCE PROJECT_WORKER_SEQ;
CREATE TABLE PROJECT_WORKER (
  id         INT  NOT NULL   PRIMARY KEY,
  Project_Id INTEGER,
  worker_Id  INTEGER,
  FOREIGN KEY (Project_Id) REFERENCES Project (id) ON DELETE CASCADE,
  FOREIGN KEY (worker_Id) REFERENCES PERSON (id)
);

CREATE SEQUENCE ATTACH_SEQ;
CREATE TABLE  ATTACH (
  id         INT   PRIMARY KEY,
  name       VARCHAR(30),
  data       TEXT,
  data2      TEXT
);

CREATE SEQUENCE IgorsMap_SEQ;
CREATE TABLE  IgorsMap (
  id         INT  PRIMARY KEY,
  upperName       VARCHAR(30),
  data       TEXT
);

INSERT INTO ATTACH (ID, name, data) VALUES (-1, 'test02',null);

INSERT INTO ATTACH (ID, name, data) VALUES (-2, 'test01',
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

INSERT INTO ATTACH (ID, name, data) VALUES (-3, 'test04',
'{"name":"hello",
  "value": 100500,
   "bool": true
 }');

INSERT INTO ATTACH (ID, name, data2) VALUES (-4, 'test05',
'{"name":"hella",
  "part": {
  "foo": "unknown",
  "bar": "314"
 }
}');

INSERT INTO ATTACH (ID, name, data) VALUES (-5, 'test06',
'{
  "name":"John",
  "age":30,
  "cars":[ "Ford", "BMW", "Fiat" ]
}');

INSERT INTO ATTACH (ID,  name, data) VALUES (-6,'test07',
'{
  "name":"Ivan",
  "age":33,
  "cars":[ "Ziguli"]
}');



/*CREATE TABLE  TEMPORAL1 (
  id         INT   PRIMARY KEY,
  name       VARCHAR(30),
  startDate    DATE default '-infinity',
  endDate     DATE default 'infinity'
);
INSERT INTO TEMPORAL1 (name) VALUES ('test01');*/


CREATE TABLE  UUID1 (
  id         UUID PRIMARY KEY NOT NULL,
  name       VARCHAR(30)
);


CREATE TABLE  WEATHER (
  id              UUID  PRIMARY KEY NOT NULL,
  name            varchar (100) NOT NULL,
  startDate       DATE NOT NULL,
  endDate         DATE NOT NULL
);

CREATE TABLE  DAY_WEATHER (
  id              UUID  PRIMARY KEY NOT NULL,
  name            varchar (100) NOT NULL,
  startDate       DATE NOT NULL,
  endDate         DATE NOT NULL,
  WEATHER_ID      UUID  NOT NULL,
  FOREIGN KEY (WEATHER_ID) REFERENCES WEATHER (id) ON DELETE CASCADE
);

CREATE SEQUENCE PLANT_TYPE_SEQ;

CREATE TABLE PLANT_TYPE (
  id        INT  PRIMARY KEY  NOT NULL ,
  name      VARCHAR(30) UNIQUE
);

CREATE SEQUENCE PLANT_SEQ;
CREATE TABLE PLANT (
  id       INT  NOT NULL  PRIMARY KEY  NOT NULL,
  name      VARCHAR(30),
  type_id      VARCHAR(30),
  FOREIGN KEY (type_id) REFERENCES PLANT_TYPE (name) ON DELETE CASCADE
);

CREATE SEQUENCE DATE_TEST_SEQ;
CREATE TABLE DATE_TEST(
  id          INT  PRIMARY KEY NOT NULL ,
  tdate date,
  "timestamp" timestamp
);

--/******************************************************
--/*****STATEMACHINE TESTS
--/******************************************************

CREATE SEQUENCE DOGOVOR_SEQ;
CREATE TABLE DOGOVOR (
  id         INT   PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  description  VARCHAR(50)
);

CREATE SEQUENCE DOGOVOR2_SEQ;
CREATE TABLE DOGOVOR2 (
  id         INT   PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  state_id      INTEGER  NOT NULL,
  description  VARCHAR(50)
);

CREATE SEQUENCE DOGOVOR3_SEQ;
CREATE TABLE DOGOVOR3 (
  id         INT   PRIMARY KEY  NOT NULL ,
  state_id      INTEGER  NOT NULL,
  description  VARCHAR(50)
);

CREATE SEQUENCE GUESS_NUMBER_SEQ;
CREATE TABLE GUESS_NUMBER (
  id         INT   PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  tryCount  INTEGER
);



CREATE TABLE NATION (
  id         UUID PRIMARY KEY  NOT NULL,
  record_Id  UUID NOT NULL,
  name      VARCHAR(30),
  start_Date DATE ,
  end_Date DATE
);

CREATE TABLE CAPITAL (
  id          UUID PRIMARY KEY  NOT NULL,
  record_id   UUID,
  name       VARCHAR(30),
  start_date DATE NOT NULL,
  end_date   DATE NOT NULL,
  nation_record_id  UUID NOT NULL,
  FOREIGN KEY (nation_record_id) REFERENCES NATION (id) ON DELETE CASCADE
);

CREATE TABLE NATION_REGISTRY (
  id         UUID PRIMARY KEY  NOT NULL,
  record_id  UUID ,
  name      VARCHAR(100),
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  nation_record_id  UUID NOT NULL,
  FOREIGN KEY (nation_record_id ) REFERENCES NATION (id) ON DELETE CASCADE
);



--/******************************************************
--/*****ТЕСТЫ НА DBDIFF
--/******************************************************


CREATE TABLE TBD0 (
  id        INTEGER PRIMARY KEY
);

CREATE TABLE TBD01 (
  id        INTEGER PRIMARY KEY,
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

CREATE TABLE PREF_ANIMAL (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)

);


CREATE TABLE PREF_LEG (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  ANIMAL_ID INTEGER,
  FOREIGN KEY (ANIMAL_ID) REFERENCES PREF_ANIMAL (id) ON DELETE CASCADE
);


CREATE VIEW PERSON_VIEW
  AS
    SELECT id, name FROM PERSON  p;

CREATE VIEW PERSON_VIEW2
  AS
    SELECT id, name FROM PERSON  p;

CREATE TABLE MONUMENT (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  PERSON_ID INTEGER
);


CREATE TABLE REMAP01 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

CREATE TABLE REMAP01_CHILD (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  first_id INTEGER,
  second_id INTEGER,
  FOREIGN KEY (first_id) REFERENCES REMAP01 (id) ON DELETE CASCADE,
  FOREIGN KEY (second_id) REFERENCES REMAP01 (id) ON DELETE CASCADE
);


CREATE TABLE REMAP02 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

CREATE TABLE REMAP02_CHILD (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  first_id INTEGER,
  second_id INTEGER,
  FOREIGN KEY (first_id) REFERENCES REMAP01 (id)
);

CREATE SEQUENCE REMAP_STRING_TO_JSON_SEQ;
CREATE TABLE REMAP_STRING_TO_JSON(
  id        INT     NOT NULL PRIMARY KEY,
  data VARCHAR(2000)  NOT NULL
);


CREATE SEQUENCE  FOO_BAR_SEQ;
CREATE TABLE FOO_BAR (
  id         INT PRIMARY KEY NOT NULL,
  name       VARCHAR(30),
  "value"    INTEGER
);

CREATE TABLE IMP04 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

CREATE TABLE IMP05 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30)
);

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

CREATE TABLE IMP02 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  imp01_id INTEGER,
  FOREIGN KEY (imp01_id) REFERENCES IMP01 (id) ON DELETE CASCADE
);

CREATE TABLE IMP03 (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  imp01_id INTEGER,
  FOREIGN KEY (imp01_id) REFERENCES IMP01 (id) ON DELETE CASCADE
);

CREATE TABLE HAND (
  id       INT PRIMARY KEY,
  name     VARCHAR(30) UNIQUE,
  name2     VARCHAR(30),
  name3     VARCHAR(30)
);
ALTER TABLE HAND ADD CONSTRAINT constraint_xxx UNIQUE (name2, name3);


CREATE TABLE FINGER (
  id         INT PRIMARY KEY,
  name       VARCHAR(30),
  hand_id    INT NOT NULL,
  FOREIGN KEY (hand_id) REFERENCES HAND (id)
);


CREATE TABLE HAND2 (
  id       INT PRIMARY KEY,
  name     VARCHAR(30)
);


CREATE TABLE HAND3 (
  id       INT PRIMARY KEY,
  name     VARCHAR(30)
);
COMMENT ON TABLE HAND3 IS 'I am tha hand';

INSERT INTO HAND(id, name) VALUES (1, 'left');
INSERT INTO FINGER(id, name, hand_id) VALUES (1, 'noname', 1);