{
  "name": "Holding",
  "scan-fields-in-db": true,
  //просканируй базу самостоятельно и вытяни сам инфу по всем полям,
  "default-group": "name, reestrObjType, immovableProperty, listNumber, startDate, endDate, kni",
  "fields": {
    //это дефолтная группа полей. вроде мы описали дефолтную? да, тут просто уточняем маппинги
    //например для рефересных полей
    "reestrObjType": {
      "m-1": {
        "to": "ObjectType",
        "join-column": "reestrObjType_id"
      }
    }
  },
  "groups": {
    "collections":{
      "documents": //на самом деле это M-M c перевязочной сущностью
      {
        "1-m": {
          "to": "DocObject",
          "their-join-column": "holding_id"
        }
      },
      "primaryObjects":
      {
        "1-m": {
          "to": "ObjectObject",
          "their-join-column": "primaryObject_id"
        }
      },
      "lawInstances":
      {
        "1-m": {
          "to": "LawInstanceObj",
          //что вытаскивать из структуры  LawInstanceObj (а хочется положим не все)
           "resolve": "lawinstance{ caption, lawInstanceTypeName, extID, startDate, endDate}",
           "their-join-column": "lawInstance_id"
        }
      }
    }
  }
}