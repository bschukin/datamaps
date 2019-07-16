INSERT INTO CITY (ID, TITLE) VALUES (1, 'Moscow');
INSERT INTO CITY (ID, TITLE) VALUES (2, 'Saint Petersburg');
INSERT INTO CITY (ID, TITLE) VALUES (3, 'Novosibirsk');

INSERT INTO PERSON (ID, name, email, gender_Id, city_id) VALUES (1, 'Madonna', 'madonna@google.com', 1, 1);
INSERT INTO PERSON (ID, name, email, gender_Id, city_id) VALUES (2, 'John Lennon', 'john@google.com', 2, 2);
INSERT INTO PERSON (ID, name, email, gender_Id, city_id) VALUES (3, 'Fillip Bedrosovich', 'filya@google.com', 3, 3);
INSERT INTO PERSON (ID, name, email, gender_Id, city_id) VALUES (4, 'Oleg Gazmanov', 'gazman@google.com', 4, 1);
INSERT INTO PERSON (ID, name, email, gender_Id, city_id) VALUES (5, 'Mylene Farmer', 'mylene@francetelecom.fr', 1, 2);

INSERT INTO Department (ID, name, parent_Id, city_id, boss_id) VALUES (1, 'Департамент', NULL, 1, 2);
INSERT INTO Department (ID, name, parent_Id, city_id, boss_id) VALUES (2, 'Отдел', 1, 1, 1);
INSERT INTO Department (ID, name, parent_Id, city_id, boss_id) VALUES (3, 'Сектор', 2, 2, 1);
INSERT INTO Department (ID, name, parent_Id, city_id, boss_id) VALUES (4, 'Пара', 2, 1, 1);
INSERT INTO Department (ID, name, parent_Id, city_id, boss_id) VALUES (5, 'Человечек', 4, 1, 1);
INSERT INTO Department (ID, name, parent_Id, city_id, boss_id) VALUES (6, 'ГородЗеро', NULL, 1, 2);

INSERT INTO Project (ID, name) VALUES (1, 'SAUMI');
INSERT INTO Project (ID, name) VALUES (2, 'QDP');

INSERT INTO TASK (ID, name, project_id) VALUES (1, 'SAUMI-001', 1);
INSERT INTO TASK (ID, name, project_id) VALUES (2, 'SAUMI-002', 1);

INSERT INTO TASK (ID, name, project_id) VALUES (3, 'QDP-003', 2);
INSERT INTO TASK (ID, name, project_id) VALUES (4, 'QDP-004', 2);

INSERT INTO PROJECT_WORKER (ID, project_id, worker_Id) VALUES (1, 2, 1);
INSERT INTO PROJECT_WORKER (ID, project_id, worker_Id) VALUES (2, 2, 2);

INSERT INTO WORKER_Department (ID, WORKER_Id, department_id) VALUES (1, 2, 1);
INSERT INTO WORKER_Department (ID, WORKER_Id, department_id) VALUES (2, 2, 2);

INSERT INTO CHECKLIST (ID, name, task_id) VALUES (1, 'foo check', 3);
INSERT INTO CHECKLIST (ID, name, task_id) VALUES (2, 'bar check', 3);


INSERT INTO PREF_ANIMAL (ID, name) VALUES (1, 'DOG');

INSERT INTO PREF_LEG (ID, name, ANIMAL_ID) VALUES (1, 'Right', 1);
INSERT INTO PREF_LEG (ID, name, ANIMAL_ID) VALUES (2, 'Left', 1);

INSERT INTO MONUMENT (ID, name, PERSON_ID) VALUES (1, 'John the Unknown', 2);

INSERT INTO GAME (ID, NAME, metacriticId) VALUES ('HEROES', 'HEROES', 555 );
INSERT INTO GAME (ID, NAME, metacriticId) VALUES ('SAPIOR', 'SAPER', 666);

INSERT INTO GAME_EPISODE (ID, NAME, GAME_ID) VALUES ('HEROES2', 'HEROES 2', 'HEROES');
INSERT INTO GAME_EPISODE (ID, NAME, GAME_ID) VALUES ('HEROES3', 'HEROES 3', 'HEROES');
INSERT INTO GAME_EPISODE (ID, NAME, GAME_ID) VALUES ('SAPIOR666', 'SAPIOR-FOREVER', 'SAPIOR');

INSERT INTO FILM (ID, NAME, film_type) VALUES (-1, 'War And Peace', 'Drama');
INSERT INTO FILM (ID, NAME, film_type) VALUES (-2, 'Rembo', 'Boevik');
INSERT INTO FILM (ID, NAME, film_type) VALUES (-3, 'The Ring', 'Unknown--Enum--Value');


UPDATE PERSON SET favorite_game_id = 'SAPIOR' WHERE id = 4;

INSERT INTO REMAP01 (ID, name) VALUES (1, 'MASTER');

INSERT INTO REMAP01_CHILD (ID, name, FIRST_ID) VALUES (1, 'First 01', 1);
INSERT INTO REMAP01_CHILD (ID, name, FIRST_ID) VALUES (2, 'First 02', 1);
INSERT INTO REMAP01_CHILD (ID, name, SECOND_ID) VALUES (3, 'Second 01', 1);


INSERT INTO REMAP02 (ID, name) VALUES (1, 'MASTER');

INSERT INTO REMAP02_CHILD (ID, name, FIRST_ID) VALUES (1, 'First 01', 1);
INSERT INTO REMAP02_CHILD (ID, name, FIRST_ID) VALUES (2, 'First 02', 1);
INSERT INTO REMAP02_CHILD (ID, name, SECOND_ID) VALUES (3, 'Second 01', 1);

INSERT INTO PLANT_TYPE(ID, NAME) VALUES (-1, 'tree');
INSERT INTO PLANT_TYPE(ID, NAME) VALUES (-2, 'flower');
INSERT INTO PLANT_TYPE(ID, NAME) VALUES (-3, 'grass');


INSERT INTO PLANT(ID, NAME, type_id) VALUES (-1, 'bereza', 'tree');
INSERT INTO PLANT(ID, NAME, type_id) VALUES (-2, 'klen', 'tree');
INSERT INTO PLANT(ID, NAME, type_id) VALUES (-3, 'roza','flower');