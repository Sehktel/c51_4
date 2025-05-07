(ns c51cc.parser
  (:require [c51cc.lexer :as lexer]
            [c51cc.logger :as log]))


;;==================================================
;; Определяем структуру AST узлов
;;==================================================

;; Программа содержит список объявлений
(defrecord Program [declarations]) 
;; Объявление функции содержит
;; тип возвращаемого значения, имя, параметры, вектор прерываний, вектор использования, тело
(defrecord FunctionDecl [return-type name params interrupt using body]) 
;; Объявление переменной содержит тип, имя
(defrecord VarDecl [type name]) ;; Присвоение значения на этапе объявления не поддерживается
;; Оператор присваивания содержит левую и правую часть
(defrecord Assign [left right])
;; Блок содержит список операторов
(defrecord Block [statements]) 
;; Оператор возврата содержит выражение
(defrecord Return [expr]) 
;; Бинарное выражение содержит оператор и два операнда
(defrecord BinaryExpr [operator left right]) 
;; Унарное выражение содержит оператор и один операнд
(defrecord UnaryExpr [operator operand]) 
;; Литерал содержит тип и значение
(defrecord Literal [type value])
;; Идентификатор содержит имя
(defrecord Identifier [name])
;; Объявление SFR содержит имя и адрес
(defrecord SfrDecl [name address])
;; Объявление SBIT содержит имя и бит
(defrecord SbitDecl [name bit])
;; Указатель содержит базовый тип
(defrecord PointerType [base-type])
;; Массив содержит базовый тип и размеры
(defrecord ArrayType [base-type dimensions])

;;Цикл for содержит начальное значение, конечное значение, шаг, тело
(defrecord ForLoop [init end step body])
;;Цикл while содержит условие, тело
(defrecord WhileLoop [condition body])
;;Цикл do-while содержит тело, условие
(defrecord DoWhileLoop [body condition])





;; Структура содержит имя и поля
;; (defrecord StructType [name fields]) ;; не поддерживается
;; Поле структуры содержит тип и имя
;; (defrecord StructField [type name]) ;; не поддерживается


;; Состояние парсера
(defrecord ParserState [tokens position])

;;==================================================
;; Парсер
;;==================================================

;; Предварительные объявления всех функций
(declare parse-program ;; парсер для программы
        ;;  parse-function-declaration ;; парсер для объявлений функций
        ;;  parse-function-params ;; парсер для параметров функций
        ;;  parse-return ;; парсер для операторов return
        ;;  parse-interrupt-declaration ;; парсер для объявлений прерываний
        ;;  parse-using-declaration ;; парсер для объявлений using
        ;;  parse-sfr-declaration ;; парсер для объявлений SFR
        ;;  parse-sbit-declaration ;; парсер для объявлений SBIT
        ;;  parse-identifier ;; парсер для идентификаторов
        ;;  parse-type ;; парсер для типов
        ;;  expect-token) ;; проверка токена
         parser ;; Основной парсер -- публичное API





         parse-expression ;; парсер для выражений
         parse-binary-expression ;; основной парсер
         parse-primary-expression ;; парсер для простых выражений
         parse-statement ;; парсер для операторов
         parse-block ;; парсер для блоков
         parse-function-declaration ;; парсер для объявлений функций
         parse-pointer-type ;; парсер для указателей
         parse-array-type ;; парсер для массивов
         parse-function-params ;; парсер для параметров функций
         parse-return ;; парсер для операторов return
         parse-interrupt-declaration ;; парсер для объявлений прерываний
         parse-using-declaration ;; парсер для объявлений using
         parse-sfr-declaration ;; парсер для объявлений SFR
         parse-sbit-declaration ;; парсер для объявлений SBIT
         parse-identifier ;; парсер для идентификаторов
         parse-type ;; парсер для типов
         expect-token) ;; проверка токена

;; Основной парсер
(def parse-expression 
  "Основной парсер выражений. 
   Абстракция над binary-expression для гибкости и расширяемости."
  parse-binary-expression)

;; Функции для работы с состоянием парсера
(defn current-token [state]
  (when (< (:position state) (count (:tokens state)))
    (nth (:tokens state) (:position state))))

(defn peek-next-token [state]
  (when (< (inc (:position state)) (count (:tokens state)))
    (nth (:tokens state) (inc (:position state)))))

(defn advance [state]
  (update state :position inc))

;; Функции-предикаты для проверки токенов
(defn type-keyword? [token]
  (= :type-keyword (:type token)))

(defn identifier? [token]
  (= :identifier (:type token)))

(defn c51-keyword? [token]
  (= :c51-keyword (:type token)))

;; Базовые функции парсера
(defn expect-token [state expected-type expected-value]
  ;; Получаем текущий токен из состояния парсера
  (let [token (current-token state)]
    ;; Проверяем, соответствует ли тип и значение токена ожидаемым
    (when (and (= (:type token) expected-type)
               (= (:value token) expected-value))
      ;; Если токен соответствует, переходим к следующему токену
      (advance state))))

;; Приоритет операций
(def operator-precedence
  {
   "="  1, ;; присваивание
   "+=" 2, "-=" 2, ;; сложение и вычитание
   "||" 3, ;; логическое ИЛИ
   "&&" 4, ;; логическое И
   "|"  5, "|=" 5, ;; побитовое ИЛИ
   "^"  6, "^=" 6, ;; побитовое исключающее ИЛИ
   "&"  6, "&=" 6, ;; побитовое И
   "==" 7, "!=" 7, ;; сравнение
   "<=" 7, ">=" 7, ;; сравнение
   "<"  7, ">"  7, ;; сравнение
   "+"  8, "-"  8, ;; сложение и вычитание
   "*"  9, "/" 9, ;; умножение и деление
   "%"  10, ;; остаток от деления
   "++" 11, "--" 11;; инкремент и декремент
   }) 

;; Проверяем, является ли токен оператором
(defn binary-operator? [token]
  (contains? operator-precedence (:value token)))

;; Парсинг бинарных выражений
(defn parse-binary-expression 
  ;; Перегрузка функции с двумя вариантами вызова
  ([state] (parse-binary-expression state 0))
  ([state min-precedence]
   ;; Парсим первый операнд (левую часть выражения)
   (let [[state1 left] (parse-primary-expression state)]
     ;; Используем рекурсивный цикл для обработки последующих операторов
     (loop [current-state state1
            current-left left]
       (let [op-token (current-token current-state)]
         ;; Проверяем условия для продолжения парсинга:
         ;; 1. Есть текущий токен
         ;; 2. Токен является бинарным оператором
         ;; 3. Приоритет оператора >= минимальному приоритету
         (if (and op-token 
                  (binary-operator? op-token)
                  (>= (get operator-precedence (:value op-token)) min-precedence))
           (let [op-precedence (get operator-precedence (:value op-token))
                 ;; Переходим к следующему токену после оператора
                 next-state (advance current-state)
                 ;; Рекурсивно парсим правую часть с повышенным приоритетом
                 [new-state right] (parse-binary-expression next-state (inc op-precedence))]
             ;; Продолжаем цикл с новым состоянием и новым левым операндом (бинарное выражение)
             (recur new-state
                    (->BinaryExpr (:value op-token) current-left right)))
           ;; Если условия не выполнены, возвращаем текущее состояние и левый операнд
           [current-state current-left]))))))

(defn parse-primary-expression [state]
  ;; Получаем текущий токен из состояния парсера
  (let [token (current-token state)]
    ;; Используем условную логику для разбора различных типов примитивных выражений
    (cond
      ;; Обработка числовых литералов
      (= (:type token) :number)
      [(advance state) (->Literal :number (:value token))]
      
      ;; Обработка идентификаторов (переменных)
      (identifier? token)
      [(advance state) (->Identifier (:value token))]
      
      ;; Обработка скобок и вложенных выражений
      (= (:type token) :bracket)
      (if (= (:value token) "(")
        (let [state1 (advance state)
              ;; Рекурсивный вызов parse-expression для разбора внутреннего выражения
              [state2 expr] (parse-expression state1)
              ;; Проверка закрывающей скобки
              state3 (expect-token state2 :bracket ")")]
          ;; Возвращаем состояние после разбора и разобранное выражение
          [state3 expr])
        ;; Если скобка не открывающая, возвращаем исходное состояние
        [state nil])
      
      ;; Обработка неизвестных токенов
      :else
      [state nil])))

;; Парсер идентификаторов
;; Преобразует токен идентификатора в абстрактное синтаксическое представление
;; Входные параметры:
;;   - state: текущее состояние парсера с токенами
;; Возвращает:
;;   - Кортеж [новое-состояние идентификатор] или nil
(defn parse-identifier [state]
  ;; Получаем текущий токен из состояния
  (let [token (current-token state)]
    ;; Проверяем, является ли токен идентификатором
    (when (identifier? token)
      ;; Возвращаем новое состояние и узел идентификатора
      [(advance state) (->Identifier (:value token))])))

;; Парсер указателей с устранением левой рекурсии
;; Обрабатывает многоуровневые указатели, например: int***, char**
;; Входные параметры:
;;   - state: текущее состояние парсера
;; Возвращает:
;;   - Кортеж [новое-состояние тип-указателя]
(defn parse-pointer-type [state]
  ;; Итеративный проход по токенам указателей
  (loop [current-state state
         pointer-depth 0]
    (let [token (current-token current-state)]
      (if (= (:value token) "*")
        ;; Увеличиваем глубину указателя при встрече '*'
        (recur (advance current-state) (inc pointer-depth))
        ;; Парсим базовый тип после всех указателей
        (let [[final-state base-type] (parse-type current-state)]
          ;; Создаем вложенные типы указателей через reduce
          [final-state (reduce (fn [type _] (->PointerType type))
                             base-type
                             (range pointer-depth))])))))

;; Парсер размерностей массива
;; Обрабатывает многомерные массивы, например: int[10][20]
;; Входные параметры:
;;   - state: текущее состояние парсера
;; Возвращает:
;;   - Кортеж [новое-состояние список-размерностей]
(defn parse-array-dimensions [state]
  ;; Итеративное накопление размерностей
  (loop [current-state state
         dimensions []]
    (let [token (current-token current-state)]
      (if (= (:value token) "[")
        (let [state1 (advance current-state)
              ;; Парсим размер измерения как бинарное выражение
              [state2 size] (parse-expression state1)
              ;; Проверяем закрывающую скобку
              state3 (expect-token state2 :bracket "]")]
          ;; Рекурсивно накапливаем размерности
          (recur state3 (conj dimensions size)))
        ;; Возвращаем накопленные размерности
        [current-state dimensions]))))

;; Парсер типов массивов
;; Обрабатывает объявления массивов с различными базовыми типами
;; Входные параметры:
;;   - initial-state: начальное состояние парсера
;; Возвращает:
;;   - Кортеж [новое-состояние тип-массива]
(defn parse-array-type [initial-state]
  ;; Парсим базовый тип массива
  (let [[state1 base-type] (parse-type initial-state)
        ;; Парсим размерности массива
        [state2 dimensions] (parse-array-dimensions state1)]
    ;; Создаем узел типа массива
    [state2 (->ArrayType base-type dimensions)]))

;; Парсер типов
;; Универсальный парсер для различных типов: базовые, указатели, массивы
;; Входные параметры:
;;   - state: текущее состояние парсера
;; Возвращает:
;;   - Кортеж [новое-состояние распознанный-тип]
(defn parse-type [state]
  (let [token (current-token state)]
    (cond
      ;; Обработка указателей
      (= (:value token) "*")
      (parse-pointer-type state)
      
      ;; ;; Структуры
      ;; (= (:value token) "struct")
      ;; (parse-struct-type state)
      
      ;; Обработка базовых типов
      (type-keyword? token)
      (let [base-state (advance state)
            next-token (current-token base-state)]
        (if (= (:value next-token) "[")
          ;; Если после типа идет '[', это массив
          (parse-array-type state)
          ;; Иначе просто возвращаем базовый тип
          [(advance state) (:value token)]))
      
      ;; Обработка неизвестных типов
      :else
      [state nil])))

;; (defn parse-identifier [state]
;;   (let [token (current-token state)]
;;     (when (identifier? token)
;;       [(advance state) (->Identifier (:value token))])))

;; C51-специфичные парсеры
;; (defn parse-interrupt-declaration [state]
;;   (let [state1 (expect-token state :c51-keyword "interrupt")
;;         [state2 number] (parse-expression state1)
;;         [state3 func] (parse-function-declaration state2)]
;;     [state3 (->InterruptDecl number func)]))

(defn parse-sfr-declaration [state]
  (let [state1 (expect-token state :c51-keyword "sfr")
        [state2 name-node] (parse-identifier state1)
        state3 (expect-token state2 :assignment-operator "=")
        [state4 addr] (parse-expression state3)
        state5 (expect-token state4 :separator ";")]
    [state5 (->SfrDecl (:name name-node) addr)]))

;; TODO: добавить поддержку sbit
;; (defn parse-sbit-declaration [state]
;;   (let [state1 (expect-token state :c51-keyword "sbit")
;;         [state2 name-node] (parse-identifier state1)
;;         state3 (expect-token state2 :assignment-operator "=")
;;         [state4 sfr-name] (parse-identifier state3)
;;         state5 (expect-token state4 :separator "^")
;;         [state6 bit-num] (parse-expression state5)
;;         state7 (expect-token state6 :separator ";")]
;;     [state7 (->SbitDecl (:name name-node) (:name sfr-name) bit-num)]))

(defn parse-function-params [state]
  (loop [current-state state
         params []]
    (let [token (current-token current-state)]
      (cond
        (= (:type token) :close-round-bracket)
        [current-state params]
        
        (type-keyword? token)
        (let [[state1 param-type] (parse-type current-state)
              [state2 param-name] (parse-identifier state1)
              next-token (current-token state2)]
          (if (= (:value next-token) ",")
            (recur (advance state2) (conj params {:type param-type :name (:name param-name)}))
            [state2 (conj params {:type param-type :name (:name param-name)})]))
        
        :else
        [current-state params]))))

(defn parse-function-declaration [initial-state]
  (let [[state1 return-type] (parse-type initial-state)
        [state2 name-node] (parse-identifier state1)
        state3 (expect-token state2 :bracket "(")
        [state4 params] (parse-function-params state3)
        state5 (expect-token state4 :bracket ")")
        state6 (expect-token state5 :c51-keyword "interrupt")
        [state7 interrupt] (parse-identifier state6)
        state8 (expect-token state7 :c51-keyword "using")
        [state9 using] (parse-identifier state8)
        [state10 body] (parse-block state9)]
    [state10 (->FunctionDecl return-type (:name name-node) params interrupt using body)]))

(defn parse-block [state]
  (let [
        ;; Ожидание открывающей фигурной скобки
        state1 (expect-token state :bracket "{")
        
        ;; Рекурсивный парсинг операторов внутри блока
        [state2 statements] 
        (loop [current-state state1
               stmts []]
          (let [token (current-token current-state)]
            (if (= (:value token) "}")
              ;; Завершение при закрывающей скобке
              [(advance current-state) stmts]
              ;; Рекурсивное накопление операторов
              (let [[new-state stmt] (parse-statement current-state)]
                (recur new-state (conj stmts stmt))))))]
    
    ;; Создание блока с накопленными операторами
    [state2 (->Block statements)]))

;; Парсер оператора возврата (return)
;; Обрабатывает конструкции вида: return expression;
;; Входные параметры:
;;   - state: текущее состояние парсера
;; Возвращает:
;;   - Кортеж [новое-состояние узел-возврата]
(defn parse-return [state]
  ;; Пропускаем токен 'return'
  (let [state1 (advance state)
        ;; Парсим выражение после return как бинарное выражение
        ;; Это позволяет обрабатывать сложные выражения: return x + y;
        [state2 expr] (parse-expression state1)
        
        ;; Проверяем наличие точки с запятой в конце оператора
        state3 (expect-token state2 :separator ";")]
    
    ;; Создаем узел возврата с разобранным выражением
    [state3 (->Return expr)]))

;; Универсальный парсер операторов
;; Распознает различные типы операторов в языке C51
;; Входные параметры:
;;   - state: текущее состояние парсера
;; Возвращает:
;;   - Кортеж [новое-состояние распознанный-оператор]
(defn parse-statement [state]
  ;; Получаем текущий токен для определения типа оператора
  (let [token (current-token state)]
    (cond
      ;; C51-специфичные конструкции: обработка прерываний
      (and (c51-keyword? token) (= (:value token) "interrupt"))
      (parse-interrupt-declaration state)
      
      ;; C51-специфичные конструкции: объявление SFR-регистров
      (and (c51-keyword? token) (= (:value token) "sfr"))
      (parse-sfr-declaration state)
      
      ;; C51-специфичные конструкции: объявление битовых регистров
      (and (c51-keyword? token) (= (:value token) "sbit"))
      (parse-sbit-declaration state)
      
      ;; Стандартные конструкции языка C
      ;; Оператор возврата
      (= (:value token) "return")
      (parse-return state)
      
      ;; Объявление функции или переменной
      (type-keyword? token)
      (parse-function-declaration state)
      
      ;; Обработка выражений по умолчанию
      ;; Любое выражение, завершающееся точкой с запятой
      :else
      (let [
            ;; Парсим бинарное выражение (может быть присваивание, вызов функции и т.д.)
            [new-state expr] (parse-expression state)
            
            ;; Проверяем наличие точки с запятой в конце выражения
            final-state (expect-token new-state :separator ";")]
        
        ;; Возвращаем состояние после парсинга и распознанное выражение
        [final-state expr]))))

;; Парсер программы - корневая функция синтаксического анализа
;; Преобразует последовательность токенов в абстрактное синтаксическое дерево (AST)
;;
;; Входные параметры:
;;   - tokens: Коллекция токенов, полученных от лексического анализатора
;;
;; Возвращает:
;;   - Структуру Program, содержащую список деклараций (функции, объявления и т.д.)
;;
;; Ключевые особенности:
;; - Использует рекурсивный цикл для обхода всех токенов
;; - Применяет parse-statement для разбора каждой декларации
;; - Накапливает декларации в векторе
;; - Обеспечивает полный разбор исходного кода
(defn parse-program [tokens]
  ;; Используем рекурсивный цикл для итеративного парсинга
  (loop [
         ;; Начальное состояние парсера с позицией 0
         state (->ParserState tokens 0)
         
         ;; Вектор для накопления деклараций
         declarations []]
    
    ;; Условие завершения цикла - достижение конца токенов
    (if (>= (:position state) (count tokens))
      ;; Создаем финальную структуру программы со всеми декларациями
      (->Program declarations)
      
      ;; Рекурсивный парсинг очередной декларации
      (let [
            ;; Разбираем следующий оператор/декларацию
            [new-state decl] (parse-statement state)]
        
        ;; Продолжаем цикл с обновленным состоянием и добавленной декларацией
        (recur new-state (conj declarations decl))))))

;; Публичное API
(defn parse [input]
  (let [tokens (lexer/tokenize input)]
    (parse-program tokens)))

(comment
  ;; Пример использования:
  (parse "int main() { return 0; }")
  ;; => #c51cc.parser.Program{
  ;;      :declarations [#c51cc.parser.FunctionDecl{
  ;;        :return-type "int"
  ;;        :name "main"
  ;;        :params []
  ;;        :body #c51cc.parser.Block{
  ;;          :statements [#c51cc.parser.ReturnStmt{
  ;;            :expr #c51cc.parser.Literal{
  ;;              :type :number
  ;;              :value 0}}]}}]})}

  ;; Пример C51-специфичного кода:
  (parse "
    sfr P0 = 0x80;
    sbit LED = P1^5;
    interrupt 1 void timer0() { P0 = 0xFF; }
  ")
)

;;===================================================
;; Парсеры для указателей и массивов
;;===================================================

;; ;; Парсер для указателей (устраняем леворекурсию через итерацию)
;; (defn parse-pointer-type [state]
;;   (loop [current-state state
;;          pointer-depth 0]
;;     (let [token (current-token current-state)]
;;       (if (= (:value token) "*")
;;         (recur (advance current-state) (inc pointer-depth))
;;         (let [[final-state base-type] (parse-type current-state)]
;;           [final-state (reduce (fn [type _] (->PointerType type))
;;                              base-type
;;                              (range pointer-depth))])))))

;; ;; Парсер для массивов (устраняем леворекурсию через итерацию)
;; (defn parse-array-dimensions [state]
;;   (loop [current-state state
;;          dimensions []]
;;     (let [token (current-token current-state)]
;;       (if (= (:value token) "[")
;;         (let [state1 (advance current-state)
;;               [state2 size] (parse-expression state1)
;;               state3 (expect-token state2 :bracket "]")]
;;           (recur state3 (conj dimensions size)))
;;         [current-state dimensions]))))

;; (defn parse-array-type [initial-state]
;;   (let [[state1 base-type] (parse-type initial-state)
;;         [state2 dimensions] (parse-array-dimensions state1)]
;;     [state2 (->ArrayType base-type dimensions)]))

;;===================================================
;; Варианты парсеров
;;===================================================
;; Версия с расширенной функциональностью
;; (def parse-expression 
;;   (fn [state]
;;     (let [
;;           ;; Предварительная обработка
;;           preprocessed-state (preprocess-state state)
;;          
;;           ;; Основной парсинг
;;           [new-state expr] (parse-binary-expression preprocessed-state)
;;          
;;           ;; Постобработка и валидация
;;           validated-expr (validate-expression expr)]
;;      
;;       ;; Возвращаем обработанный результат
;;       [new-state validated-expr])))
;; ------------------------------------------------
;; (def parse-expression 
;;   (fn [state]
;;     ;; Можно добавить дополнительную логику
;;     (let [result (parse-binary-expression state)]
;;       ;; Например, логирование, трассировка
;;       (println "Parsing expression:" result)
;;       result)))
;; ------------------------------------------------
;; Разные реализации для разных языков/диалектов
;; (defmulti parse-expression 
;;   (fn [state dialect] dialect))
;;
;; (defmethod parse-expression :c51 
;;   [state _] 
;;   (parse-binary-expression state))
;;
;; (defmethod parse-expression :forth 
;;   [state _] 
;;   (parse-forth-expression state))