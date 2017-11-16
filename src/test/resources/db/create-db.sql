
CREATE TABLE JiraGender (
  id         INTEGER PRIMARY KEY,
  gender VARCHAR(30),
  isClassic         bigint
  );

CREATE TABLE JiraWorker (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  email  VARCHAR(50),
  genderId INTEGER,
  FOREIGN KEY (genderId) REFERENCES JiraGender(id)
);

CREATE TABLE JiraStaffUnit (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  worker_Id INTEGER,
  genderId INTEGER,
  FOREIGN KEY (worker_Id) REFERENCES JiraWorker(id),
  FOREIGN KEY (genderId) REFERENCES JiraGender(id)
);

CREATE TABLE JiraDepartment (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  parentId  INTEGER,
  FOREIGN KEY (parentId) REFERENCES JiraDepartment(id)
);
comment on column JiraDepartment.name is 'Name of deparment';


CREATE TABLE JiraWorker_JiraDepartment (
  id         INTEGER PRIMARY KEY,
  jiraWorkerId INTEGER ,
  jiraDepartmentId INTEGER ,
  FOREIGN KEY (jiraWorkerId) REFERENCES JiraWorker(id) ON DELETE CASCADE,
  FOREIGN KEY (jiraDepartmentId) REFERENCES JiraDepartment(id) ON DELETE SET NULL
);

CREATE TABLE JiraProject (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30)
);

CREATE TABLE JiraTask (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  jiraProjectId INTEGER,
  FOREIGN KEY (jiraProjectId) REFERENCES JiraProject(id) ON DELETE CASCADE
);