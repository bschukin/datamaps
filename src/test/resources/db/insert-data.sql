INSERT INTO Jira_Gender (ID, GENDER, IS_CLASSIC) VALUES (1, 'woman', 1);
INSERT INTO Jira_Gender (ID, GENDER, IS_CLASSIC) VALUES (2, 'man', 1);
INSERT INTO Jira_Gender (ID, GENDER, IS_CLASSIC) VALUES (3, 'foo', 0);
INSERT INTO Jira_Gender (ID, GENDER, IS_CLASSIC) VALUES (4, 'bar', 0);


INSERT INTO Jira_Worker (ID, name, email, gender_Id) VALUES (1, 'Madonna', 'madonna@google.com', 1);
INSERT INTO Jira_Worker (ID, name, email, gender_Id) VALUES (2, 'John Lennon', 'john@google.com', 2);
INSERT INTO Jira_Worker (ID, name, email, gender_Id) VALUES (3, 'Fillip Bedrosovich', 'filya@google.com', 3);
INSERT INTO Jira_Worker (ID, name, email, gender_Id) VALUES (4, 'Oleg Gazmanov', 'gazman@google.com', 4);
INSERT INTO Jira_Worker (ID, name, email, gender_Id) VALUES (5, 'Mylene Farmer', 'mylene@francetelecom.fr', 1);


INSERT INTO Jira_Staff_Unit (ID, name, worker_Id, gender_Id) VALUES (1, 'Developer', 2, 2);
INSERT INTO Jira_Staff_Unit (ID, name, worker_Id, gender_Id) VALUES (2, 'Tester', 1, 1);
INSERT INTO Jira_Staff_Unit (ID, name, worker_Id, gender_Id) VALUES (3, 'Doctor Strange', null, null);


INSERT INTO Jira_Department (ID, name, parent_Id) VALUES (1, 'Департамент', null);
INSERT INTO Jira_Department (ID, name, parent_Id) VALUES (2, 'Отдел', 1);
INSERT INTO Jira_Department (ID, name, parent_Id) VALUES (3, 'Сектор', 2);
INSERT INTO Jira_Department (ID, name, parent_Id) VALUES (4, 'Пара', 3);

INSERT INTO Jira_Project (ID, name) VALUES (1, 'SAUMI');
INSERT INTO Jira_Project (ID, name) VALUES (2, 'QDP');

INSERT INTO Jira_TASK (ID, name, jira_project_id) VALUES (1, 'SAUMI-001', 1);
INSERT INTO Jira_TASK (ID, name, jira_project_id) VALUES (2, 'SAUMI-002', 1);

INSERT INTO Jira_TASK (ID, name, jira_project_id) VALUES (3, 'QDP-003', 2);
INSERT INTO Jira_TASK (ID, name, jira_project_id) VALUES (4, 'QDP-004', 2);

INSERT INTO JIRA_PROJECT_WORKER (ID,  jira_project_id,jira_worker_Id) VALUES (1, 2, 1);
INSERT INTO JIRA_PROJECT_WORKER (ID, jira_project_id, jira_worker_Id) VALUES (2, 2, 2);

INSERT INTO JIRA_CHECKLIST (ID, name, jira_task_id) VALUES (1, 'foo check', 3);
INSERT INTO JIRA_CHECKLIST (ID, name, jira_task_id) VALUES (2, 'bar check', 3);