DROP TABLE  IF EXISTS JIRA_GENDER CASCADE;

CREATE TABLE  JIRA_GENDER (
  id         INTEGER PRIMARY KEY,
  gender VARCHAR(30),
    IS_CLASSIC BOOLEAN DEFAULT FALSE NOT NULL
  );

DROP TABLE  IF EXISTS JIRA_WORKER CASCADE;

DROP SEQUENCE  IF EXISTS JIRA_WORKER_SEQ;
CREATE SEQUENCE JIRA_WORKER_SEQ START WITH 1000;

CREATE TABLE JIRA_WORKER (
  id         INTEGER  PRIMARY KEY,
  name VARCHAR(30),
  email  VARCHAR(50),
  gender_id INTEGER,
  FOREIGN KEY (gender_id) REFERENCES JIRA_GENDER(id)
);
DROP TABLE  IF EXISTS JIRA_STAFF_UNIT CASCADE;
CREATE TABLE JIRA_STAFF_UNIT (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  worker_Id INTEGER,
  gender_Id INTEGER,
  FOREIGN KEY (worker_Id) REFERENCES JIRA_WORKER(id),
  FOREIGN KEY (gender_Id) REFERENCES JIRA_GENDER(id)
);

DROP TABLE  IF EXISTS Jira_Department CASCADE;

CREATE TABLE Jira_Department (
  id         IDENTITY PRIMARY KEY,
  name VARCHAR(30),
  parent_Id  INTEGER,
  FOREIGN KEY (parent_Id) REFERENCES Jira_Department(id)
);
comment on column Jira_Department.name is 'Name of deparment';

DROP TABLE  IF EXISTS JIRA_WORKER_Jira_Department CASCADE;


CREATE TABLE JIRA_WORKER_Jira_Department (
  id         INTEGER PRIMARY KEY,
  JIRA_WORKER_Id INTEGER ,
  jira_Department_Id INTEGER ,
  FOREIGN KEY (JIRA_WORKER_Id) REFERENCES JIRA_WORKER(id) ON DELETE CASCADE,
  FOREIGN KEY (jira_Department_Id) REFERENCES Jira_Department(id) ON DELETE SET NULL
);

DROP TABLE  IF EXISTS JIRA_PROJECT CASCADE;

CREATE TABLE JIRA_PROJECT (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30)
);

DROP TABLE  IF EXISTS JIRA_TASK CASCADE;


CREATE TABLE JIRA_TASK (
  id         IDENTITY PRIMARY KEY,
  name VARCHAR(30),
  jira_Project_Id INTEGER,
  FOREIGN KEY (jira_Project_Id) REFERENCES Jira_Project(id) ON DELETE CASCADE
);

DROP TABLE  IF EXISTS JIRA_CHECKLIST CASCADE;

CREATE TABLE JIRA_CHECKLIST (
  id         IDENTITY PRIMARY KEY,
  name VARCHAR(30),
  JIRA_TASK_ID INTEGER,
  FOREIGN KEY (JIRA_TASK_ID) REFERENCES JIRA_TASK(id) ON DELETE CASCADE
);