{
  "name":"JiraWorker",
  "scan-fields-in-db":true,   //просканируй базу самостоятельно и вытяни сам инфу по всем полям
  "default-group": "name, surname, patronymic, snils", //(опция) только эти поля будут загружаться когда мы загрущзаем сущность
                                                      //если дефолт-группа  не указана - тянутся все поля
  "ref-group": "name, surname, snils"           //(опция) только эти поля будут тянуться при использовании JiraWorker в качестве ссылки
                                                //по  умолчанию - используется "default-group"
}