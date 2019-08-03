# DataMaps

DataMaps is a lightweight ORM-framework for Kotlin. 
https://app.gitbook.com/@ice9/s/project/datamaps-reference/osnovy/chto-takoe-datamaps


1. DataMaps - часть фреймворка ICE компании БФТ.
    ICE  - мульти-модуль прожект на котлин, 
    на клиентской части используется  reactjs on kotlin. 
    
    в ICE как таковом, датамапс определяет основные объекты и api 
    в common модуле (для работы с картами и на сервере и на клиенте).

   
2. в данной сборке datamaps собран как серверный фреймворк,
   common-часть перенесена в  корень пакетов 
   com.bftcom.ice.datamaps и com.bftcom.ice.datamaps.misc

    Пакет com.bftcom.ice.datamaps.core 
    является реализацией предосставляемого api.
    
3. Тесты делятся на две категории:
    - examples - примеры и прецедеденты использования
    - интеграционные тесты на фреймворк
 
4. также из ICE принесен пример реализации  статусной модели (finite state machine).

4. ICE (и Datamaps в частности) уже используется в нескольких проектах в проде. 
   Datamaps зарекомендовал себя как легкий, быстрый и маложрущий память фремйворк.       
