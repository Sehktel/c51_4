# Заметки по парсеру c51cc

## Различие между VarDecl и Identifier

### VarDecl
- Представляет объявление переменной
- Содержит два поля: `type` и `name`
- Используется при первичном внесении переменной в таблицу символов
- Пример: `int x;`
  ```clojure
  (->VarDecl "int" "x")
  ```

### Identifier
- Представляет ссылку на ранее объявленную переменную
- Содержит одно поле: `name`
- Используется при использовании переменной в выражениях
- Пример: `x + 5`
  ```clojure
  (->Identifier "x")
  ```

### Ключевые различия
- VarDecl — внесение в таблицу символов (binding)
- Identifier — использование ранее объявленной переменной (reference)

## Литералы в AST

### Зачем нужен Literal
1. Представление константных значений
2. Семантический анализ и типизация
3. Возможность оптимизаций на этапе компиляции
4. Упрощение генерации кода
5. Расширяемость парсера

### Пример
```clojure
(->Literal :number 42)
```

## Структуры данных

### SbitDecl
- Представляет объявление специфического бита
- Содержит два поля: `name`, `bit`
- Используется для работы с битами в регистрах микроконтроллера

### Interrupt и другие специфические структуры
- Позволяют точно описывать особенности целевой платформы (C51) 