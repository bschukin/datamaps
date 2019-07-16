CREATE OR REPLACE PROCEDURE CREATE_COLUMN_IF_NOT_EXISTS (l_table_name VARCHAR2, l_column_name VARCHAR2, l_column_script VARCHAR2) IS
  l_check  VARCHAR2(10);
  BEGIN
    SELECT 'VALID' into l_check FROM all_tab_columns where table_name = l_table_name and column_name = l_column_name;
    EXCEPTION WHEN NO_DATA_FOUND then
    EXECUTE IMMEDIATE  'ALTER TABLE ' || l_table_name || ' ADD ' || l_column_name || ' '||  l_column_script;
  END;
/

CREATE OR REPLACE PROCEDURE CREATE_CONSTR_IF_NOT_EXISTS (table_name VARCHAR2, aconstraint_name VARCHAR2, constraint_script VARCHAR2) IS
  l_check  VARCHAR2(10);
  BEGIN
    SELECT 'VALID' into l_check FROM all_constraints where UPPER(constraint_name) = UPPER(aconstraint_name);
    EXCEPTION WHEN NO_DATA_FOUND then
    EXECUTE IMMEDIATE  'ALTER TABLE ' || table_name || ' ADD CONSTRAINT ' || aconstraint_name  || ' '||  constraint_script;
  END;
/

CREATE OR REPLACE PROCEDURE CREATE_SEQUENCE_IF_NOT_EXISTS (isSeqName VARCHAR2) IS
 lnSeqCount NUMBER;
BEGIN
  -- try to find sequence in data dictionary
  SELECT count(1)
  INTO lnSeqCount
  FROM user_sequences
  WHERE sequence_name = UPPER(isSeqName);
  -- if sequence not found, create it
  IF lnSeqCount = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE ' || isSeqName || ' START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE CACHE 20 NOORDER';
  END IF;
END;
/

CREATE OR REPLACE procedure CREATE_SEQUENCE_IF_NOT_EXISTS2 (isSeqName VARCHAR2, startWith NUMBER) IS
lnSeqCount NUMBER;
BEGIN
 -- try to find sequence in data dictionary
 SELECT count(1)
 INTO lnSeqCount
 FROM user_sequences
 WHERE sequence_name = UPPER(isSeqName);
 -- if sequence not found, create it
 IF lnSeqCount = 0 THEN
   EXECUTE IMMEDIATE 'CREATE SEQUENCE ' || isSeqName || ' START WITH ' || TO_CHAR(startWith) || ' INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE CACHE 20 NOORDER';
 END IF;
END;
/

CREATE OR REPLACE PROCEDURE DROP_SEQUENCE_IF_EXIST (sequence_name VARCHAR2) IS
BEGIN
  EXECUTE IMMEDIATE 'DROP SEQUENCE ' || sequence_name;
  EXCEPTION WHEN OTHERS THEN IF SQLCODE != -2289 THEN RAISE; END IF;
END;
/

CREATE OR REPLACE PROCEDURE CREATE_IDENTITY_IF_NOT_EXISTS(l_table_name VARCHAR2) IS
  seqName  VARCHAR2(50);
  trigName VARCHAR2(50);
  BEGIN
    seqName := l_table_name || '_SEQ';
    trigName := l_table_name || '_BRI';
    create_sequence_if_not_exists(seqName);
   EXECUTE IMMEDIATE
    'CREATE OR REPLACE TRIGGER ' || trigName || ' BEFORE INSERT ON ' || l_table_name ||chr(10)||
    ' REFERENCING OLD AS OLD NEW AS NEW '||chr(10)||
    ' FOR EACH ROW '||chr(10)||
    ' BEGIN ' ||chr(10)||
    '   :NEW.ID := COALESCE(:NEW.ID,'|| seqName ||'.NEXTVAL); ' ||chr(10)||
    ' END;';
  END;
/

CREATE OR REPLACE PROCEDURE CREATE_TABLE_IF_NOT_EXISTS (l_table_name VARCHAR2, table_body VARCHAR2) IS
  l_check  VARCHAR2(10);
  BEGIN
    SELECT 'VALID' into l_check FROM all_tables where table_name = l_table_name;
    EXCEPTION WHEN NO_DATA_FOUND then
    EXECUTE IMMEDIATE 'create table '|| l_table_name || table_body;
  END;
/

CREATE  OR REPLACE  PROCEDURE DROP_TABLE_IF_EXIST (l_table_name VARCHAR2) IS
  l_check  VARCHAR2(10);
  BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE ' ||upper(l_table_name) || ' CASCADE CONSTRAINTS';
    EXCEPTION
      WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;

  END;
/

CREATE OR REPLACE PROCEDURE CREATE_SEQUENCE_IF_NOT_EXISTS3 (isSeqName VARCHAR2, body VARCHAR2) IS
lnSeqCount NUMBER;
BEGIN
  -- try to find sequence in data dictionary
  SELECT count(1)
  INTO lnSeqCount
  FROM user_sequences
  WHERE sequence_name = UPPER(isSeqName);
  -- if sequence not found, create it
  IF lnSeqCount = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE ' || isSeqName || ' ' || body;
  END IF;
END;
/
