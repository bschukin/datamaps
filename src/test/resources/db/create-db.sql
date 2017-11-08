
CREATE TABLE JiraGender (
  id         INTEGER PRIMARY KEY,
  gender VARCHAR(30),
  isClassic         INTEGER
  );

CREATE TABLE JiraWorker (
  id         INTEGER PRIMARY KEY,
  name VARCHAR(30),
  email  VARCHAR(50),
  jiraGenderId INTEGER,
  FOREIGN KEY (jiraGenderId) REFERENCES JiraGender(id) ON DELETE CASCADE
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

