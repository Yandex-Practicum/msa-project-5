### <a name="_b7urdng99y53"></a>**Название задачи: Реализация ETL с использованием Spring Batch** 
### <a name="_hjk0fkfyohdk"></a>**Автор: Евгений Соболев**
### <a name="_uanumrh8zrui"></a>**Дата: 07.02.2026**
### <a name="_3bfxc9a45514"></a>**Функциональные требования**

| **№** | **Действующие лица или системы** | **Use Case**      | **Описание**                                 |
|:-----:|:---------------------------------|:------------------|:---------------------------------------------|
|   1   | Сотрудник склада                 | Загрузка отчёта   | Пользователь загружает CSV-файл через ERP    |     
|   2   | ERP-система                      | Инициация ETL     | Передача задания в сервис                    |      
|   3   | Spring Batch                     | Валидация данных  | Валидация данных                             |      
|   4   | Spring Batch                     | Обогащение данных | Обогащение данных                            |      
|   5   | Spring Batch                     | Сохранение данных | Сохранение данных в БД Postgres              |      
|   6   | Spring Batch                     | Информирование    | Уведомление пользователя о статусе обработки |      
|   7   | Admin                            | Рестарт job       | Перезапуск неуспешных job                    |      

### <a name="_u8xz25hbrgql"></a>**Нефункциональные требования**

| **№** | **Требование**                                                                     |
|:-----:|:-----------------------------------------------------------------------------------|
|   1   | Обработка до 400000 строк/сутки. Включая пиковые нагрузки с увеличением в 2-3 раза |
|   2   | Поддержка не менее 100 параллельных загрузок                                       |
|   3   | Обработка файла из 2000 строк менее чем за 30 секунд                               |
|   4   | Отказоустойчивость и возможность restart                                           |
|   5   | Поддержка retry для ошибочных записей                                              |
|   6   | Централизованное логирование                                                       |
|   7   | Мониторинг выполнения job                                                          |
|   8   | Возможность гибкого масштабирования под нагрузкой                                  |
|   9   | Гарантия целостности данных                                                        |


### <a name="_qmphm5d6rvi3"></a>**Решение**
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml
title TradeWare - Context Diagram
    Person(user, "Сотрудник склада", "Загружает отчёты и получает статус")
    System(erp, "ERP System", "Монолитная система TradeWare")
    System(batch, "ETL Batch Service", "Spring Batch сервис обработки данных")
    SystemDb(db, "PostgreSQL", "Хранилище данных")
    System_Ext(gcs, "Google Cloud Storage", "Хранение CSV-файлов")
    
    Rel(user, erp, "Работает через UI")
    Rel(erp, batch, "Запускает ETL Job", "REST / Async")
    Rel(batch, db, "Читает / Записывает", "JDBC")
    Rel(erp, gcs, "Сохраняет файлы", "API")
    Rel(batch, gcs, "Читает файлы", "API")
@enduml

@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml
title TradeWare - Container Diagram
    Person(user, "Сотрудник склада")
    System_Boundary(tradeware, "TradeWare System") {
    Container(web, "Web UI", "Angular", "Интерфейс пользователя")
    Container(api, "ERP Backend", "Java / WildFly", "Бизнес-логика и API")
    Container(batch, "Batch Service", "Spring Boot + Spring Batch", "ETL обработка файлов")
    ContainerDb(db, "PostgreSQL", "RDBMS", "Основная БД")
    Container_Ext(gcs, "Google Cloud Storage", "Object Storage", "Хранилище файлов")
    Container_Ext(monitor, "Monitoring", "Prometheus + Grafana", "Мониторинг и алерты")
}

    Rel(user, web, "Использует")
    Rel(web, api, "REST / WebSocket")
    Rel(api, batch, "Запуск Job", "REST / Queue")
    Rel(api, gcs, "Загрузка файлов")
    Rel(batch, gcs, "Чтение CSV")
    Rel(batch, db, "Read / Write", "JDBC")
    Rel(batch, monitor, "Метрики")
    Rel(api, monitor, "Метрики")
@enduml


При выборе решения учитывались:
- Используемый Java-стек.
- Опыт команды.
- Требования к надёжности.
- Необходимость restart/retry.
- Поддержка транзакций.

Spring Batch был выбран как наиболее подходящее решение, обеспечивающее зрелую batch-архитектуру и глубокую интеграцию со Spring.### <a name="_bjrr7veeh80c"></a>**Альтернативы**

Альтернативы

| **Решение**      | **Преимущества**     | **Недостатки**                     |
|:-----------------|:---------------------|:-----------------------------------|
| Apache Airflow   | Гибкая оркестрация   | Python-стек, сложность поддержки   |

**Недостатки, ограничения, риски**

Недостатки:
- Необходимость поддержки Job Repository.
- Увеличение DevOps-нагрузки.

Ограничения:
- Зависимость от производительности PostgreSQL.
- Отсутствие real-time обработки.

| **Риск**            | **Описание**         | **Контр-мера**                              |
|:--------------------|:---------------------|:--------------------------------------------|
| Ошибки данных       | Некорректные CSV     | Валидация                                   |
| Рост нагрузки       | Рост нагрузки        | Kubernetes autoscaling                      |
| Сбои job            | Прерывание обработки | Restart                                     |
| Высокая нагрузка БД | Высокая нагрузка БД  | Индексация, партиционирование, шардирование |


