# playtox_test

Тестовое задание для Playtox.

Тут всего один класс (PlaytoxTest). Там вся логика.
На классы не разбивал, т.к. задача не про архитектуру, а про многопоточность, уровни изоляции, блокировки, ...

Цепляется к локальному postgres (jdbc:postgresql://localhost:5432/playtox_db) (но если будет нужно - заверну в докер, напишите)
(нужно создать роль playtox_app и базу playtox_db):

```
CREATE USER playtox_app WITH ENCRYPTED PASSWORD 'playtox';
CREATE DATABSE playtox_db OWNER playtox_app;
```

В корне пример лога: playtox_test.log
