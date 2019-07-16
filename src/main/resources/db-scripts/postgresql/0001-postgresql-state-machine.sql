CREATE TABLE IF NOT EXISTS  {{StateMachineInstance}} (
  id        BIGSERIAL PRIMARY KEY NOT NULL,
  stateMachineId INTEGER NOT NULL,
  stateId INTEGER NOT NULL,
  created timestamp NOT NULL,
  changed timestamp,
  FOREIGN KEY (stateMachineId) REFERENCES {{StateMachine}} (id),
  FOREIGN KEY (stateId) REFERENCES {{STATE}} (id)
);