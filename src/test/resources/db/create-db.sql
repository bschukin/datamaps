
CREATE TABLE JIRA_GENDER (
  id         INTEGER PRIMARY KEY,
  gender VARCHAR(30),
  IS_CLASSIC         bigint
  );

CREATE TABLE JIRA_WORKER (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  email  VARCHAR(50),
  gender_id INTEGER,
  FOREIGN KEY (gender_id) REFERENCES JIRA_GENDER(id)
);

CREATE TABLE JIRA_STAFF_UNIT (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  worker_Id INTEGER,
  gender_Id INTEGER,
  FOREIGN KEY (worker_Id) REFERENCES JIRA_WORKER(id),
  FOREIGN KEY (gender_Id) REFERENCES JIRA_GENDER(id)
);

CREATE TABLE Jira_Department (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  parent_Id  INTEGER,
  FOREIGN KEY (parent_Id) REFERENCES Jira_Department(id)
);
comment on column Jira_Department.name is 'Name of deparment';


CREATE TABLE JIRA_WORKER_Jira_Department (
  id         INTEGER PRIMARY KEY,
  JIRA_WORKER_Id INTEGER ,
  jira_Department_Id INTEGER ,
  FOREIGN KEY (JIRA_WORKER_Id) REFERENCES JIRA_WORKER(id) ON DELETE CASCADE,
  FOREIGN KEY (jira_Department_Id) REFERENCES Jira_Department(id) ON DELETE SET NULL
);

CREATE TABLE JIRA_PROJECT (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30)
);

CREATE TABLE JIRA_TASK (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  jira_Project_Id INTEGER,
  FOREIGN KEY (jira_Project_Id) REFERENCES Jira_Project(id) ON DELETE CASCADE
);