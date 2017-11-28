{
  "entity": "JiraDepartment",
  "table": "DEPARTMENT",
  "fields": [         //это дефолтная группа полей
    {
      "field": "title",
      "type": "string"
    },
    {
      "field": "parentDepartment",
      "m-1": {
        "to": "JiraDepartment",
        "join-column": "parentDepartment_id"
      }
    }
  ],
  "groups": [
    {
      "name": "full",           //эту группу будем включать для edit-формы.
      "fields": [
        {
          "field": "projects",
          "type": "list",
          "m-m": {
            "to": "JiraProject",
            "join-table": "department_project",
            "our-join-column": "department_id",
            "their-join-column": "project_id"
          }
        },
        {
          "field": "workers",
          "m-m": {
            "to": "JiraWorker",
            "join-table": "department_worker",
            "our-join-column": "department_id1",
            "their-join-column": "worker_id"
          }
        }
      ]
    }
  ]
}