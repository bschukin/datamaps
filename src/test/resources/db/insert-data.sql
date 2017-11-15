INSERT INTO JiraGender (ID, GENDER, ISCLASSIC) VALUES (1, 'woman', 1);
INSERT INTO JiraGender (ID, GENDER, ISCLASSIC) VALUES (2, 'man', 1);
INSERT INTO JiraGender (ID, GENDER, ISCLASSIC) VALUES (3, 'foo', 0);
INSERT INTO JiraGender (ID, GENDER, ISCLASSIC) VALUES (4, 'bar', 0);


INSERT INTO JiraWorker (ID, name, email, genderId) VALUES (1, 'Madonna', 'madonna@google.com', 1);
INSERT INTO JiraWorker (ID, name, email, genderId) VALUES (2, 'John Lennon', 'john@google.com', 2);
INSERT INTO JiraWorker (ID, name, email, genderId) VALUES (3, 'Fillip Bedrosovich', 'filya@google.com', 3);
INSERT INTO JiraWorker (ID, name, email, genderId) VALUES (4, 'Oleg Gazmanov', 'gazman@google.com', 4);


INSERT INTO JiraStaffUnit (ID, name, worker_Id, genderId) VALUES (1, 'Developer', 2, 2);
INSERT INTO JiraStaffUnit (ID, name, worker_Id, genderId) VALUES (2, 'Tester', 1, 1);
