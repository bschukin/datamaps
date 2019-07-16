
DROP TABLE IF EXISTS DDL_SCRIPT CASCADE;

CREATE TABLE DDL_SCRIPT (
  id        IDENTITY PRIMARY KEY,
  FILE_NAME     VARCHAR(100),
  EXECUTED  timestamp
)

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
  IS_CLASSIC BOOLEAN DEFAULT FALSE NOT NULL
);

INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (1, 'woman', 1);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (2, 'man', 1);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (3, 'foo', 0);
INSERT INTO Gender (ID, NAME, IS_CLASSIC) VALUES (4, 'bar', 0);

DROP TABLE IF EXISTS CITY CASCADE;

CREATE TABLE CITY (
  id    INTEGER PRIMARY KEY,
  title VARCHAR(30)
);

DROP TABLE IF EXISTS PERSON CASCADE;

DROP SEQUENCE IF EXISTS PERSON_SEQ;
CREATE SEQUENCE PERSON_SEQ
  START WITH 1000;

CREATE TABLE PERSON (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  email     VARCHAR(50),
  gender_id INTEGER,
  last_name VARCHAR(30),
  age       VARCHAR(50),
  city_id   INTEGER,
  bio CLOB,
  photo BLOB,
  favorite_game_id VARCHAR(30),
  FOREIGN KEY (gender_id) REFERENCES GENDER (id),
  FOREIGN KEY (city_id) REFERENCES CITY (id),
  FOREIGN KEY (favorite_game_id) REFERENCES GAME (id)
);

COMMENT ON TABLE PERSON IS 'We are the robots';

DROP TABLE IF EXISTS CHILD CASCADE;

CREATE TABLE CHILD (
  id        IDENTITY PRIMARY KEY,
  name      VARCHAR(30),
  PERSON_ID INTEGER,
  FOREIGN KEY (PERSON_ID) REFERENCES PERSON (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS Department CASCADE;

CREATE TABLE Department (
  id        IDENTITY PRIMARY KEY,
  name      VARCHAR(30),
  parent_Id INTEGER,
  city_id   INTEGER,
  boss_id INTEGER,
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


CREATE TABLE TASK (
  id         IDENTITY PRIMARY KEY,
  name       VARCHAR(30),
  project_Id INTEGER,
  FOREIGN KEY (project_Id) REFERENCES Project (id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS PROJECT_WORKER CASCADE;
CREATE TABLE PROJECT_WORKER (
  id         IDENTITY PRIMARY KEY,
  project_Id INTEGER,
  WORKER_Id  INTEGER,
  FOREIGN KEY (Project_Id) REFERENCES Project (id) ON DELETE CASCADE,
  FOREIGN KEY (WORKER_Id) REFERENCES PERSON (id)
);


DROP TABLE IF EXISTS CHECKLIST CASCADE;
CREATE TABLE CHECKLIST (
  id      IDENTITY PRIMARY KEY,
  name    VARCHAR(30),
  TASK_ID INTEGER,
  FOREIGN KEY (TASK_ID) REFERENCES TASK (id) ON DELETE CASCADE
);


DROP TABLE IF EXISTS FILM CASCADE;
CREATE TABLE FILM (
  id      IDENTITY PRIMARY KEY,
  name    VARCHAR(30),
  film_type VARCHAR(30),
  film_type2 VARCHAR(30)
);

DROP TABLE IF EXISTS ATTACH CASCADE;
CREATE TABLE  ATTACH (
  id         IDENTITY PRIMARY KEY,
  name       VARCHAR(30),
  data       VARCHAR(1000),
  data2       VARCHAR(1000)
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


DROP TABLE IF EXISTS UUID1 ;
CREATE TABLE  UUID1 (
  id         UUID  PRIMARY KEY NOT NULL,
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


DROP TABLE IF EXISTS FOO_BAR CASCADE;
CREATE TABLE FOO_BAR (
  id         IDENTITY PRIMARY KEY NOT NULL,
  name       VARCHAR(30),
  value      INTEGER
);

--/******************************************************
--/*****ТЕСТЫ НА DBDIFF
--/******************************************************
DROP TABLE  IF EXISTS TBD0 CASCADE;
CREATE TABLE TBD0 (
  id        IDENTITY PRIMARY KEY
);

DROP TABLE  IF EXISTS TBD01 CASCADE;

CREATE TABLE TBD01 (
  id        BIGINT PRIMARY KEY,
  name       VARCHAR(30),
  name2      VARCHAR(30),
  someiny    INTEGER,
  tbd0_id INTEGER,
  FOREIGN KEY (tbd0_id) REFERENCES TBD0 (id) ON DELETE CASCADE,
  tbd01_id INTEGER,
  FOREIGN KEY (tbd01_id) REFERENCES TBD0 (id) ON DELETE CASCADE,
  oldName VARCHAR (30),
  booleanField BOOLEAN

);

DROP TABLE  IF EXISTS TBD01_CHILD CASCADE;

CREATE TABLE TBD01_CHILD (
  id        BIGINT PRIMARY KEY,
  tbd01_id INTEGER,
  FOREIGN KEY (tbd01_id) REFERENCES TBD01 (id) ON DELETE CASCADE,
  tbd011_id INTEGER,
  FOREIGN KEY (tbd011_id) REFERENCES TBD01 (id) ON DELETE CASCADE
);

DROP TABLE  IF EXISTS TBD01_BABE CASCADE;

CREATE TABLE TBD01_BABE (
  id         BIGINT PRIMARY KEY,
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
CREATE VIEW PERSON_VIEW
 AS
 SELECT p.id id, p.name name FROM PERSON p;

DROP VIEW IF EXISTS PERSON_VIEW2;
CREATE VIEW PERSON_VIEW2
 AS
 SELECT p.id id, p.name name FROM PERSON p;

DROP TABLE IF EXISTS MONUMENT CASCADE;
CREATE TABLE MONUMENT (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  person_id INTEGER
);

DROP TABLE IF EXISTS MONUMENT CASCADE;
CREATE TABLE MONUMENT (
  id        INTEGER PRIMARY KEY,
  name      VARCHAR(30),
  person_id INTEGER
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


DROP SEQUENCE IF EXISTS DATE_TEST_SEQ;
CREATE SEQUENCE DATE_TEST_SEQ
START WITH 1000;

CREATE TABLE IF NOT EXISTS DATE_TEST(
  id        INTEGER PRIMARY KEY,
  tdate date,
  "timestamp"  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS REMAP_STRING_TO_JSON(
  id        IDENTITY PRIMARY KEY  NOT NULL ,
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

DROP TABLE IF EXISTS PLANT_TYPE CASCADE;
CREATE TABLE PLANT_TYPE (
  id        IDENTITY  PRIMARY KEY  NOT NULL ,
  name      VARCHAR(30) UNIQUE
);

DROP TABLE IF EXISTS PLANT CASCADE;
CREATE TABLE PLANT (
  id        IDENTITY  PRIMARY KEY  NOT NULL,
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

DROP TABLE IF EXISTS IgorsMap CASCADE;
CREATE TABLE  IgorsMap (
  id       IDENTITY  PRIMARY KEY  NOT NULL,
  upperName       VARCHAR(30),
  data       CLOB
);


--/******************************************************
--/*****STATEMACHINE TESTS
--/******************************************************

CREATE TABLE IF NOT EXISTS DOGOVOR (
  id        IDENTITY  PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  description  VARCHAR(50),
  FOREIGN KEY (state_Machine_instance_id) REFERENCES STATE_MACHINE_INSTANCE (id)
);

CREATE TABLE IF NOT EXISTS DOGOVOR2(
  id        IDENTITY  PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  state_id      INTEGER  NOT NULL,
  description  VARCHAR(50),
  FOREIGN KEY (state_id) REFERENCES STATE (id),
  FOREIGN KEY (state_Machine_instance_id) REFERENCES STATE_MACHINE_INSTANCE (id)
);

CREATE TABLE IF NOT EXISTS DOGOVOR3 (
  id        IDENTITY  PRIMARY KEY  NOT NULL ,
  state_id      INTEGER  NOT NULL,
  description  VARCHAR(50),
  FOREIGN KEY (state_id) REFERENCES STATE (id)
);

CREATE TABLE IF NOT EXISTS GUESS_NUMBER (
  id        IDENTITY  PRIMARY KEY  NOT NULL ,
  state_Machine_instance_id      INTEGER  NOT NULL,
  tryCount  INTEGER,
  FOREIGN KEY (state_Machine_instance_id) REFERENCES STATE_MACHINE_INSTANCE (id)
);

DROP TABLE IF EXISTS userpolicy CASCADE;
DROP TABLE IF EXISTS useraccount CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS userrole CASCADE;
DROP TABLE IF EXISTS Role_Permission CASCADE;
DROP TABLE IF EXISTS Role_Parent CASCADE;

create table userpolicy (
	id IDENTITY not null constraint userpolicy_pkey primary key,
	name varchar(50) not null,
	"sessionExpirationTime" integer,
	"passwordExpirationDays" integer,
	"passwordEnterCount" integer,
	"passwordMinLength" integer
);

create table useraccount (
	id IDENTITY not null constraint useraccount_pkey primary key,
	name varchar(50) not null,
	"fullName" varchar(255),
	email varchar(255),
	phone varchar(20),
	password varchar(255),
	blocked boolean,
	"blockedReason" varchar(1024),
	"passwordChangeDate" date,
	"passwordEnterCount" integer,
	mustChangePassword boolean,
	lastLoginTime TIMESTAMP,
	passwordChangeDate date,
	recieveEmails boolean default false,
    recieveSms boolean default false,
	"user_policy_id" integer constraint "UserAccount_userPolicyId_FK" references userpolicy
);

--/******************************************************
--/*****NOTIFICATION TESTS
--/******************************************************

create table role (
	id BIGINT not null primary key,
	code varchar(50) not null,
	name varchar(255) not null,
	description varchar(1024),
	isSystem boolean
);

create table userrole (
	id BIGINT not null primary key,
	userId BIGINT constraint UserRole_userId_FK references useraccount(id) on delete cascade,
    roleId BIGINT constraint UserRole_roleId_FK references role(id)
);

create table Role_Permission (
   id bigint not null primary key,
   roleId bigint constraint RolePermission_roleId_FK references role(id) on delete cascade,
   appObj character varying(50) not null,
   fieldName character varying(50),
   canCreate boolean,
   canRead boolean,
   canWrite boolean,
   canDelete boolean
);

CREATE TABLE Role_Parent (
  id  bigint NOT NULL CONSTRAINT RoleParent_PK PRIMARY KEY,
  roleId bigint CONSTRAINT RoleParent_roleId_FK REFERENCES Role ON DELETE CASCADE,
  parentId bigint CONSTRAINT RoleParent_parentId_FK REFERENCES Role
);

CREATE TABLE if not exists Notification (
    id                         UUID NOT NULL,
    name                       varchar(255),
    level                      varchar(36) NOT NULL,
    messages                   varchar(1000),
    params                     varchar(1000),
    recipients                 varchar(1000),
    created                    timestamp NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE if not exists Notification_Delivery_Log (
    id                         UUID NOT NULL,
    notificationId             UUID NOT NULL,
    userId                     BIGINT NOT NULL,
    userName                   varchar(50) NOT NULL,
    userDescription            varchar(255),
    userChannelDescription     varchar(255),
    channel                    varchar(36) NOT NULL,
    extId                      varchar(255),
    message                    varchar(1000),
    created                    timestamp NOT NULL,
    viewed                     timestamp,

    PRIMARY KEY (id),
    UNIQUE (notificationId, userId, channel),
    FOREIGN KEY (notificationId) REFERENCES Notification (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Notification_Template_Override (
    id                      UUID NOT NULL,
    templateId              varchar(36) NOT NULL,
    channel                 varchar(36) NOT NULL,
    field                   varchar(36) NOT NULL,
    value                   varchar(1000),

    PRIMARY KEY (id),
    UNIQUE (templateId, channel, field)
);

insert into role (id, code, name, isSystem) values (0, 'superUser', 'Супер пользователь', true);
insert into role (id, code, name, isSystem) values (1, 'admin', 'Администратор системы', true);
insert into userpolicy (id, name) values (0, 'DEFAULT');
insert into useraccount (id, name, "fullName", password, "user_policy_id", blocked) values (0, 'root', 'root', '$2a$10$.x78t4cmZu61WkU.apoGcOVSqFpeJ6oq5h79dYAmUOVOaFOMoB.3C', 0, false);
insert into userrole (id, userId, roleId) values (0, 0, 0);

