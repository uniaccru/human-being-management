
- HumanBeing concurrency + stress test
- Цель: проверить изоляцию транзакций и устойчивость API при параллельных CRUD/IMPORT операциях.

## Базовые настройки

- Хост: localhost
- Порт: 24180
- Базовый путь API: /human-being-manager/api
- Протокол: http
- Таймауты HTTP Defaults: connect 10000 мс, response 30000 мс
- Общие заголовки: Content-Type application/json, Accept application/json

## Сценарии

### 1) Concurrent UPDATE одного и того же объекта

- Thread Group: Test: Concurrent UPDATE - Same Object
- Нагрузка: 20 потоков, ramp-up 1 сек, 3 цикла
- Шаги:
  - создается объект;
  - извлекается id через RegexExtractor и сохраняется в property;
  - параллельно выполняются PUT-обновления одного и того же id;
  - добавляется случайная задержка 100-600 мс между апдейтами.
- Проверка: ожидается код 200.

### 2) Concurrent DELETE одного и того же объекта

- Thread Group: Test: Concurrent DELETE - Same Object
- Нагрузка: 20 потоков, ramp-up 1 сек, 1 цикл
- Шаги:
  - создается объект;
  - извлекается id и сохраняется в property;
  - все потоки пытаются удалить один и тот же объект.
- Ожидаемое поведение:
  - 1 успешный DELETE со статусом 204;
  - 19 (N-1) ответов со статусом 404

### 3) Concurrent IMPORT операций

- Thread Group: Test: Concurrent IMPORT Operations
- Нагрузка: 20 потоков, ramp-up ${RAMP_UP}, 2 цикла
- Endpoint: POST /human-being-manager/api/import/humanbeings
- Тело: JSON-массив из двух объектов с рандомизированными полями координат и данных.
- Проверка: допустимы коды 200 и 400.
  - 200: импорт успешен;
  - 400: ошибка валидации.

### 4) Full CRUD Cycle (CREATE-READ-UPDATE-DELETE)

- Thread Group: Test: Full CRUD Cycle (CREATE-READ-UPDATE-DELETE)
- Нагрузка: 20 потоков, ramp-up ${RAMP_UP}, 2 цикла
- Поток выполнения:
  - CREATE -> извлечение id;
  - READ (GET по id), проверка 200;
  - UPDATE (PUT по id), проверка 200;
  - DELETE (DELETE по id), проверка 204.
- Для READ/UPDATE/DELETE используются IfController, чтобы выполнять шаг только при корректно извлеченном id.

### 5) Concurrent CREATE с одинаковыми координатами

- Thread Group: Test: Concurrent CREATE - Same Coordinates
- Нагрузка: 20 потоков, ramp-up 1 сек, 1 цикл
- Endpoint: POST /human-being-manager/api/humanbeings
- Особенность: все потоки отправляют одинаковые coordinates (x=431, y=430.0).
- Проверка: допустимы коды 201 и 400.
  - 201: только один запрос должен создать объект;
  - 400: остальные запросы должны упасть на ограничении уникальности.
- Дополнительно: JSR223 PostProcessor считает число успешных и неуспешных ответов.

## Слушатели 

Включены listener-ы:

- Summary Report
- Aggregate Report

