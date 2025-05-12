(ns c51cc.parser
  (:require [c51cc.lexer :as lexer]
            [c51cc.logger :as log]
            [c51cc.ast.nodes :as nodes]))

;;==================================================
;; Определяем структуру AST узлов
;;==================================================

;; Программа содержит список объявлений
(defrecord Program [declarations]) 
;; Объявление функции содержит
;; тип возвращаемого значения, имя, параметры, вектор прерываний, вектор использования, тело
(defrecord FunctionDeclaration [return-type name params interrupt using body]) 
;; Объявление переменной содержит тип, имя
(defrecord VariableDeclaration [type name]) ;; Присвоение значения на этапе объявления не поддерживается
;; Оператор присваивания содержит левую и правую часть
(defrecord Assignment [left right])
;; Блок содержит список операторов
(defrecord Block [statements]) 
;; Оператор возврата содержит выражение
(defrecord ReturnStatement [expr]) 
;; Бинарное выражение содержит оператор и два операнда
(defrecord BinaryExpression [operator left right]) 
;; Унарное выражение содержит тип оператора, оператор и один операнд
(defrecord UnaryExpression [type operator operand]) 
;; Литерал содержит тип и значение
(defrecord Literal [type value])
;; Идентификатор содержит имя
(defrecord Identifier [name])
;; Объявление SFR содержит имя и адрес
(defrecord SfrDeclaration [name address])
;; Объявление SBIT содержит имя и бит
(defrecord SbitDeclaration [name bit])
;; Указатель содержит базовый тип
;; (defrecord PointerType [base-type])
;; Массив содержит базовый тип и размеры
(defrecord ArrayType [base-type dimensions])

;;Цикл for содержит начальное значение, конечное значение, шаг, тело
(defrecord ForLoop [init end step body])
;;Цикл while содержит условие, тело
(defrecord WhileLoop [condition body])
;;Цикл do-while содержит тело, условие
;;(defrecord DoWhileLoop [body condition]) ;; not implemented

;; Объявление прерывания содержит номер и функцию
(defrecord InterruptDeclaration [number func])
;; Объявление using содержит имя и список переменных
(defrecord UsingDeclaration [name vars])

;;==================================================
;; Парсер
;;==================================================

;; Предварительные объявления всех функций
(declare 
          current-token               ;;	 Возвращает текущий токен
          next-token                  ;;	 Возвращает следующий токен без перемещения позиции
          next-next-token             ;;	 Возвращает дважды следующий токен без перемещения позиции
          step-back                   ;;	 Возвращается на один токен назад
          step-next                   ;;	 Перемещается на один токен вперед
          expect-token               	;;	 Проверяет, соответствует ли текущий токен ожидаемому типу и значению, и переходит к следующему токену.
          handle-error 	              ;;	 Обработка ошибок синтаксического анализа, которая может включать вывод сообщений об ошибках и восстановление состояния парсера.
          parse-function-declaration  ;;	 Обрабатывает объявления функций, включая параметры, возвращаемый тип и тело функции.
          parse-function-params   	  ;;	 Обрабатывает список параметров функции, включая их типы и имена.
          parse-main-declaration      ;;  Обрабатывает объявление функции main
          
          
          
          parse-array-access 	        ;;	 Обрабатывает доступ к элементам массивов, включая индексацию.
          parse-array-dimensions    	;;	 Обрабатывает размерности массивов, если они присутствуют в объявлении.
          parse-array-type           	;;	 Обрабатывает объявления массивов, включая их размерности.
          parse-assignment 	          ;;	 Обрабатывает операторы присваивания, включая сложные присваивания (например, +=, -=).
          parse-binary-expression   	;;	 Обрабатывает бинарные выражения с учетом приоритета операторов.
          parse-bitwise-arithmetic 	  ;;	 Обрабатывает побитовые арифметические операции, включая побитовые И, ИЛИ, исключающее ИЛИ и их комбинации.
          parse-bitwise-expressions   ;;	 Обрабатывает битовые операции, включая побитовые И, ИЛИ, исключающее ИЛИ и их комбинации.
          parse-block 	              ;;	 Обрабатывает блоки кода, заключенные в фигурные скобки, и накапливает операторы внутри блока.
          parse-break 	              ;;	 Обрабатывает оператор break, который используется для выхода из циклов.
          parse-case 	                ;;	 Обрабатывает оператор case, который используется для выбора одного из нескольких вариантов в зависимости от значения переменной.
          parse-default 	            ;;	 Обрабатывает оператор default, который используется для выбора одного из нескольких вариантов в зависимости от значения переменной.
          parse-condition 	          ;;	 Обрабатывает условия для управляющих конструкций, таких как if, while, и for.
          parse-conditional-operator 	;;	 Обрабатывает тернарный оператор (условный оператор) ? :, если он будет поддерживаться.
          parse-constant 	            ;;	 Обрабатывает константы, включая целочисленные и символьные константы.
          parse-continue 	            ;;	 Обрабатывает оператор continue, который используется для перехода к следующей итерации цикла.
          parse-do-while-loop 	      ;;	 Специфическая процедура для разбора циклов do-while.
          parse-expression 	          ;;	 Основная функция для разбора выражений, включая бинарные и унарные выражения.
          parse-for-loop 	            ;;	 Специфическая процедура для разбора циклов for, включая инициализацию, условие и шаг.
          parse-function-call 	      ;;	 Обрабатывает вызовы функций, включая передачу аргументов и обработку возвращаемых значений.
          parse-goto 	                ;;	 Обрабатывает оператор goto, который используется для перехода к определенной метке в программе.
          parse-identifier 	          ;;	 Обрабатывает идентификаторы, проверяя их корректность и возвращая соответствующие узлы.
          parse-increment-decrement 	;;	 Обрабатывает инкременты и декременты.
          parse-interrupt-declaration ;;	 Обрабатывает объявления прерываний.
          parse-label 	              ;;	 Обрабатывает метки, которые используются для обозначения определенных точек в программе.
          parse-literal 	            ;;	 Обрабатывает различные типы литералов, такие как числовые, символьные и булевы значения.
          parse-logical-expressions   ;;	 Обрабатывает логические выражения, включая операторы && и ||.
          parse-nested-block 	        ;;	 Обрабатывает вложенные блоки кода, что может быть полезно для обработки сложных структур.
          parse-nested-statements   	;;	 Обрабатывает вложенные операторы, включая операторы if, while, и for.
          parse-primary-expression   	;;	 Обрабатывает простые выражения, такие как литералы, идентификаторы и вложенные выражения в скобках.
          parse-program 	            ;;	 Основной парсер программы, который принимает список токенов и возвращает структуру программы, содержащую все декларации.
          parse-register 	            ;;	 Обрабатывает регистры, включая регистры данных и регистры адреса.
          parse-return 	              ;;	 Обрабатывает оператор return, который используется для возврата значения из функции.
          parse-sbit-declaration   	  ;;	 Обрабатывает объявления битовых регистров.
          parse-sfr-declaration   	  ;;	 Обрабатывает объявления SFR.
          parse-statement 	          ;;	 Универсальная функция для разбора различных операторов, включая объявления функций, переменных, операторов возврата и специфичных для C51 конструкций.
          parse-switch-case          	;;	 Обрабатывает оператор switch, который используется для выбора одного из нескольких вариантов в зависимости от значения переменной.
          parse-type 	                ;;	 Обрабатывает различные типы данных, включая базовые типы и массивы.
          parse-unary-expression 	    ;;	 Обрабатывает унарные выражения, такие как инкременты и декременты.
          parse-using-declaration   	;;	 Обрабатывает объявления using, которые используются для указания переменных, которые будут использоваться в программе.
          parse-while-loop 	          ;;	 Специфическая процедура для разбора циклов while.
        ) 


;; Состояние парсера
(defrecord ParserState [tokens position])

;; Основной парсер
;; (def parse-expression 
;;   "Основной парсер выражений. 
;;    Абстракция над binary-expression для гибкости и расширяемости."
;;   parse-binary-expression)


;; ==========================================================
;; Перемещение по вектору токенов
;; ==========================================================

;; Мое текущее состояние -- это токен, на котором я сейчас нахожусь.
;; current-token -- это функция, которая возвращает текущий токен.
;; next-token -- это функция, которая возвращает следующий токен.
;; next-next-token -- это функция, которая возвращает дважды следующий токен.
;; step-back -- это функция, которая возвращается на один токен назад.
;; step-next -- это функция, которая перемещается на один токен вперед.
;; expect-token -- это функция, которая ожидает токен с ожидаемым типом и значением.
;; Получается, что я могу перемещаться по вектору токенов, по мере необходимости.

(defn current-token [state]
  (when state  ;; Guard against nil state
    (let [tokens (:tokens state)
          position (:position state)]
      (when (and tokens ;; Guard against nil tokens
                 (not-empty tokens)
                 (number? position)
                 (< position (count tokens)))
        (nth tokens position)))))

(defn next-token [state]
  (let [tokens (:tokens state)
        position (:position state)
        next-pos (inc position)]
    (if (and (< next-pos (count tokens))
             (not-empty tokens))
      (nth tokens next-pos)
      nil)))

(defn next-next-token [state]
  (let [tokens (:tokens state)
        position (:position state)
        next-pos (inc (inc position))]
    (if (and (< next-pos (count tokens))
             (not-empty tokens))
      (nth tokens next-pos)
      nil)))


(defn step-back [state]
  (let [tokens (:tokens state)
        position (:position state)
        prev-pos (dec position)]
    (when (and (< prev-pos (count tokens))
               (not-empty tokens))
      (assoc state :position prev-pos))))

(defn step-next [state]
  (let [tokens (:tokens state)
        position (:position state)
        next-pos (inc position)]
    (if (and (< next-pos (count tokens))
             (not-empty tokens))
      (assoc state :position next-pos)
      nil)))

;; Ожидаемый токен с ожидаемым типом и значением
(defn expect-token [state expected-type expected-value]
  ;; Получаем текущий токен из состояния парсера
  (let [token (current-token state)]
    ;; Проверяем, соответствует ли тип и значение токена ожидаемым
    (when (and (= (:type token) expected-type)
               (= (:value token) expected-value))
      ;; Если токен соответствует, переходим к следующему токену
      (step-next state))))
; ===================  MISC ==================================
(defn print-token [token]
  (when token
    (println (format "Type: %-20s Value: %s" 
                    (name (:type token))
                    (:value token)))))
;; Использование:
;; (print-token (current-token test-state))




; =========================================================

;; ==========================================================
;; Функции-предикаты для проверки токенов
;; ==========================================================

;; Типы переменных
(defn type-void-keyword?     [token] (= :void-type-keyword                 (:type token)))
(defn type-int-keyword?      [token] (= :int-type-keyword                  (:type token)))
(defn type-char-keyword?     [token] (= :char-type-keyword                 (:type token)))
(defn type-signed-keyword?   [token] (= :signed-type-keyword               (:type token)))
(defn type-unsigned-keyword? [token] (= :unsigned-type-keyword             (:type token)))

;; Управляющие конструкции
(defn type-if-keyword?       [token] (= :if-control-keyword                (:type token)))
(defn type-else-keyword?     [token] (= :else-control-keyword              (:type token)))
(defn type-while-keyword?    [token] (= :while-control-keyword             (:type token)))
(defn type-for-keyword?      [token] (= :for-control-keyword               (:type token)))
(defn type-return-keyword?   [token] (= :return-control-keyword            (:type token)))

;; Ключевое слово main
(defn type-main-keyword?     [token] (= :main-keyword                      (:type token)))

;; Специальные ключевые слова микроконтроллера
(defn interrupt-keyword?     [token] (= :interrupt-c51-keyword             (:type token)))
(defn sfr-keyword?           [token] (= :sfr-c51-keyword                   (:type token)))
(defn sbit-keyword?          [token] (= :sbit-c51-keyword                  (:type token)))
(defn using-keyword?         [token] (= :using-c51-keyword                 (:type token)))

;; Скобки
(defn open-round-bracket?    [token] (= :open-round-bracket                (:type token)))
(defn close-round-bracket?   [token] (= :close-round-bracket               (:type token)))
(defn open-curly-bracket?    [token] (= :open-curly-bracket                (:type token)))
(defn close-curly-bracket?   [token] (= :close-curly-bracket               (:type token)))
(defn open-square-bracket?   [token] (= :open-square-bracket               (:type token)))
(defn close-square-bracket?  [token] (= :close-square-bracket              (:type token)))

;; Операторы сравнения
(defn greater?               [token] (= :greater-comparison-operator       (:type token)))
(defn less?                  [token] (= :less-comparison-operator          (:type token)))
(defn greater-equal?         [token] (= :greater-equal-comparison-operator (:type token)))
(defn less-equal?            [token] (= :less-equal-comparison-operator    (:type token)))
(defn not-equal?             [token] (= :not-equal-comparison-operator     (:type token)))

;; Операторы присваивания
(defn equal?                 [token] (= :equal-assignment-operator         (:type token)))
(defn and-equal?             [token] (= :and-equal-assignment-operator     (:type token)))
(defn or-equal?              [token] (= :or-equal-assignment-operator      (:type token)))
(defn xor-equal?             [token] (= :xor-equal-assignment-operator     (:type token)))

;; Битовые операторы
(defn and-bitwise?           [token] (= :and-bitwise-operator              (:type token)))
(defn or-bitwise?            [token] (= :or-bitwise-operator               (:type token)))
(defn xor-bitwise?           [token] (= :xor-bitwise-operator              (:type token)))
(defn not-bitwise?           [token] (= :not-bitwise-operator              (:type token)))

;; Разделители
(defn semicolon?             [token] (= :semicolon-separator               (:type token)))
(defn comma?                 [token] (= :comma-separator                   (:type token)))
(defn dot?                   [token] (= :dot-separator                     (:type token)))
(defn colon?                 [token] (= :colon-separator                   (:type token)))

;; Арифметические операторы
(defn plus?                  [token] (= :plus-math-operator                (:type token)))
(defn minus?                 [token] (= :minus-math-operator               (:type token)))
(defn multiply?              [token] (= :multiply-math-operator            (:type token)))
(defn divide?                [token] (= :divide-math-operator              (:type token)))
(defn modulo?                [token] (= :modulo-math-operator              (:type token)))


;; Инкремент и декремент
(defn increment?             [token] (= :increment-operator                (:type token)))
(defn decrement?             [token] (= :decrement-operator                (:type token)))

;; Логические операторы
(defn or-logical?            [token] (= :or-logical-operator               (:type token)))
(defn and-logical?           [token] (= :and-logical-operator              (:type token)))
(defn equal-logical?         [token] (= :equal-logical-operator            (:type token)))
(defn not-equal-logical?     [token] (= :not-equal-logical-operator        (:type token)))
(defn not-logical?           [token] (= :not-logical-operator              (:type token)))

;; Числа
(defn int-number?            [token] (= :int-number                        (:type token)))
(defn hex-number?            [token] (= :hex-number                        (:type token)))

;; Идентификатор
(defn identifier?            [token] (= :identifier                        (:type token)))

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

(defn handle-error
  "Обработчик ошибок парсера на основе конечного автомата.
   Состояния:
   :initial -> начальное состояние
   :recovery -> восстановление после ошибки
   :sync -> синхронизация с ближайшим разделителем
   :done -> завершение обработки"
  [state & [error-info]]
  (let [fsm-state (atom :initial)
        sync-tokens #{:semicolon-separator :close-curly-bracket :close-round-bracket}
        token (current-token state)]
    
    ;; Логируем информацию об ошибке
    (log/error "Ошибка парсера:"
               "\n  - Текущий токен:" (when token 
                                       {:type (:type token) 
                                        :value (:value token)})
               "\n  - Позиция:" (:position state)
               "\n  - Контекст:" error-info
               "\n  - Предыдущий AST:" (:ast state))
    
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case @fsm-state
          :initial 
          (do 
            (log/debug "Начало обработки ошибки")
            (reset! fsm-state :recovery)
            (recur (step-next current-state)))  ;; Важно! Продвигаемся вперед
          
          :recovery
          (do 
            (log/debug "Попытка восстановления после ошибки")
            (reset! fsm-state :sync)
            (recur (step-next current-state)))  ;; Важно! Продвигаемся вперед
          
          :sync
          (let [token (current-token current-state)]
            (cond
              (nil? token) 
              (do
                (log/debug "Достигнут конец файла во время восстановления")
                (step-next current-state))
              
              (sync-tokens (:type token))
              (do
                (log/debug "Найден токен синхронизации:" (:type token))
                (reset! fsm-state :done)
                (step-next current-state))
              
              :else 
              (do
                (log/trace "Пропуск токена:" (:type token))
                (recur (step-next current-state)))))
          
          :done 
          (do
            (log/debug "Восстановление завершено")
            (step-next current-state)))))))

(defn parser [state]
  (parse-program state))


(defn parse-main-function [state] (state))

(defn parse-program
  "Основной парсер программы.
   Принимает вектор токенов и возвращает AST программы."
  [tokens]
  (log/trace "Entering parse-program with tokens count:" (count tokens))
  (let [initial-state (->ParserState tokens 0)
        fsm-state (atom {:stage :start 
                        :declarations []})]
    (loop [current-state initial-state]
      (let [token (current-token current-state)]
        (log/debug "parse-program: Processing token:" 
                  (when token 
                    {:type (:type token) 
                     :value (:value token)
                     :position (:position current-state)}))
        
        (cond
          ;; Конец файла или nil state
          (or (nil? token)
              (nil? current-state))
          (do
            (log/trace "parse-program: Parsing complete. Declarations found:" 
                      (count (:declarations @fsm-state)))
            (nodes/->Program (:declarations @fsm-state)))
          
          ;; Закрывающая фигурная скобка - это часть блока, пропускаем
          (close-curly-bracket? token)
          (if-let [next-state (step-next current-state)]
            (do
              (log/debug "parse-program: Skipping closing brace, moving to position:" 
                        (:position next-state))
              (recur next-state))
            ;; Если step-next вернул nil, значит достигнут конец файла
            (nodes/->Program (:declarations @fsm-state)))
          
          ;; Тип данных - начало новой декларации
          (or (type-void-keyword? token)
              (type-int-keyword? token)
              (type-char-keyword? token)
              (type-unsigned-keyword? token)
              (type-signed-keyword? token))
          (let [decl-state (parse-statement current-state)]
            (if (:ast decl-state)
              (do 
                (swap! fsm-state update :declarations conj (:ast decl-state))
                (recur decl-state))
              (handle-error current-state 
                          {:context "Failed to parse declaration"
                           :token token})))
          
          ;; Идентификатор - возможно присваивание
          (identifier? token)
          (let [assign-state (parse-assignment current-state)]
            (if (:ast assign-state)
              (do
                (swap! fsm-state update :declarations conj (:ast assign-state))
                (recur assign-state))
              (handle-error current-state 
                          {:context "Failed to parse assignment"
                           :token token})))
          
          ;; Специальные ключевые слова микроконтроллера
          (sfr-keyword? token)
          (let [sfr-state (parse-sfr-declaration current-state)]
            (if (:ast sfr-state)
              (do
                (swap! fsm-state update :declarations conj (:ast sfr-state))
                (recur sfr-state))
              (handle-error current-state 
                          {:context "Failed to parse SFR declaration"
                           :token token})))
          
          (sbit-keyword? token)
          (let [sbit-state (parse-sbit-declaration current-state)]
            (if (:ast sbit-state)
              (do
                (swap! fsm-state update :declarations conj (:ast sbit-state))
                (recur sbit-state))
              (handle-error current-state 
                          {:context "Failed to parse SBIT declaration"
                           :token token})))
          
          ;; Пропускаем все остальные токены
          :else
          (recur (step-next current-state)))))))

(defn parse-function-params
  "Парсер параметров функции.
   Обрабатывает:
   - Тип параметра (void/int/char)
   - Имя параметра (если есть)
   - Закрывающую скобку
   
   Возвращает вектор параметров и новое состояние"
  [state]
  (let [token (current-token state)]
    (log/trace "Entering parse-function-params. Current token:" 
               (when token {:type (:type token) :value (:value token)})
               "Position:" (:position state))
    (let [param-type-token (current-token state)]
      (if (type-void-keyword? param-type-token)
        ;; Если void - параметров нет
        (do 
          (log/trace "parse-function-params: No parameters (void)")
          [(step-next state) []])
        ;; TODO: Здесь будет обработка других параметров
        (do 
          (log/trace "parse-function-params: Placeholder for parameter parsing")
          [(step-next state) []])))))

(defn parse-main-declaration
  "Специализированный парсер для функции main.
   Обрабатывает особенности объявления главной функции программы."
  [state]
  (let [token (current-token state)]
    (log/trace "Entering parse-main-declaration. Current token:" 
               (when token {:type (:type token) :value (:value token)})
               "Position:" (:position state))
    
    ;; Получаем тип возврата из AST предыдущего состояния
    (let [return-type (:type (:ast state))
          main-token (current-token state)
          _ (log/debug "Main declaration - Return type:" return-type 
                       "Main token:" (when main-token 
                                     {:type (:type main-token) 
                                      :value (:value main-token)}))]
      
      (if (and return-type (type-main-keyword? main-token))
        (let [;; Переходим к открывающей скобке параметров
              params-state (step-next state)
              params-token (current-token params-state)
              _ (log/debug "Params token:" 
                           (when params-token 
                             {:type (:type params-token) 
                              :value (:value params-token)}))]
          
          (if (open-round-bracket? params-token)
            (let [;; Начинаем парсить параметры
                  param-start-state (step-next params-state)
                  [after-params-state params] (parse-function-params param-start-state)
                  
                  ;; Проверяем закрывающую скобку параметров
                  close-paren-token (current-token after-params-state)
                  _ (log/debug "Close paren token:" 
                               (when close-paren-token 
                                 {:type (:type close-paren-token) 
                                  :value (:value close-paren-token)}))
                  
                  ;; Переходим к телу функции
                  body-state (when (close-round-bracket? close-paren-token)
                              (step-next after-params-state))
                  
                  ;; Парсим блок тела функции
                  [after-body-state body] (when (and body-state 
                                                   (open-curly-bracket? (current-token body-state)))
                                           (parse-block body-state))]
              
              (if (and (close-round-bracket? close-paren-token)
                       body)
                ;; Успешный парсинг main функции
                (assoc after-body-state 
                       :ast 
                       (nodes/->FunctionDeclaration 
                         return-type
                         "main"
                         params
                         nil    ;; interrupt
                         nil    ;; using
                         body)) ;; body теперь содержит блок
                
                ;; Ошибка в скобках
                (do 
                  (log/error "Ошибка в объявлении функции main: неверные скобки"
                             "\n  - Закрывающая скобка:" (when close-paren-token (:type close-paren-token))
                             "\n  - Тело функции:" (when body body))
                  (handle-error state {:context "Invalid main function declaration structure"}))))
            
            ;; Отсутствует открывающая скобка
            (do 
              (log/error "Ошибка в объявлении функции main: отсутствует открывающая скобка параметров")
              (handle-error state {:context "Missing opening parenthesis in main function declaration"}))))
        
        ;; Неверный тип возврата или отсутствие main
        (do 
          (log/error "Ошибка в объявлении функции main:"
                     "\n  - Тип возврата:" return-type
                     "\n  - Токен main:" (when main-token (:type main-token)))
          (handle-error state {:context "Invalid main function declaration"}))))))

(defn parse-function-declaration
  "Расширенный парсер объявления функции с улучшенной обработкой ошибок.
   Поддерживает различные типы возвращаемых значений и параметров."
  [state]
  (let [token (current-token state)]
    (log/trace "Entering parse-function-declaration. Current token:" 
               (when token {:type (:type token) :value (:value token)})
               "Position:" (:position state))
    
    ;; Если текущий токен - main, используем специализированный парсер
    (if (type-main-keyword? (current-token state))
      (parse-main-declaration state)
      
      (let [return-type-token (current-token state)
            return-type (cond 
                         (type-void-keyword? return-type-token) :void
                         (type-int-keyword? return-type-token) :int
                         (type-char-keyword? return-type-token) :char
                         (type-unsigned-keyword? return-type-token) :unsigned
                         (type-signed-keyword? return-type-token) :signed
                         :else nil)
            
            ;; Шаг 2: Переходим к имени функции
            name-state (step-next state)
            name-token (current-token name-state)
            
            ;; Шаг 3: Проверяем открывающую скобку параметров
            params-state (step-next name-state)
            params-token (current-token params-state)]
        
        (if (and return-type 
                 (identifier? name-token)
                 (open-round-bracket? params-token))
          (let [[params-list new-state] (parse-function-params (step-next params-state))
                interrupt-decl (when (interrupt-keyword? (current-token new-state))
                               (parse-interrupt-declaration new-state))
                using-decl (when (using-keyword? (current-token (or (:state interrupt-decl) new-state)))
                            (parse-using-declaration (or (:state interrupt-decl) new-state)))
                body-state (parse-block (or (:state using-decl) 
                                          (:state interrupt-decl) 
                                          new-state))]
            
            (if (:ast body-state)
              (assoc body-state :ast 
                     (nodes/->FunctionDeclaration 
                       return-type
                       (:value name-token)
                       params-list
                       (:ast interrupt-decl)
                       (:ast using-decl)
                       (:ast body-state)))
              (handle-error state 
                          {:context "Failed to parse function body"
                           :token (current-token body-state)})))
          
          (handle-error state 
                      {:context "Invalid function declaration"
                       :expected "return-type identifier ( params )"
                       :found {:return-type return-type
                              :name (when name-token 
                                     {:type (:type name-token)
                                      :value (:value name-token)})
                              :params (when params-token 
                                      {:type (:type params-token)
                                       :value (:value params-token)})}}))))))

(defn parse-sfr-declaration
  "Заглушка для парсера SFR.
   Просто возвращает тестовый AST для проверки работы основного парсера"
  [state]
  (let [token (current-token state)]
    (log/trace "Entering parse-sfr-declaration. Current token:" 
               (:type token) (:value token)
               "Position:" (:position state))
    (-> state
        step-next  ;; Продвигаемся к следующему токену
        (assoc :ast 
               (nodes/->SfrDeclaration 
                 "TEST_SFR"    ;; name
                 0x80)))))     ;; address

(defn parse-sbit-declaration
  "Заглушка для парсера SBIT.
   Просто возвращает тестовый AST для проверки работы основного парсера"
  [state]
  (let [token (current-token state)]
    (log/trace "Entering parse-sbit-declaration. Current token:" 
               (:type token) (:value token)
               "Position:" (:position state))
    (-> state
        step-next  ;; Продвигаемся к следующему токену
        (assoc :ast 
               (nodes/->SbitDeclaration 
                 "TEST_SBIT"   ;; name
                 0)))))        ;; bit

(defn parse-type
  "Обрабатывает различные типы данных, включая базовые типы и массивы.
   Поддерживает signed и unsigned модификаторы."
  [state]
  (log/trace "Entering parse-type with state:" 
             {:position (:position state)
              :current-token (when-let [t (current-token state)] 
                             {:type (:type t) :value (:value t)})})
  (let [token (current-token state)]
    (log/trace "Entering parse-type. Current token:" 
               (when token {:type (:type token) :value (:value token)})
               "Position:" (:position state))
    (let [next-token (next-token state)
          next-next-token (next-next-token state)]
      (log/trace "parse-type details. Next token:" 
                 (when next-token {:type (:type next-token) :value (:value next-token)})
                 "Next-next token:" 
                 (when next-next-token {:type (:type next-next-token) :value (:value next-next-token)}))
      (cond
        ;; Unsigned int
        (and (type-unsigned-keyword? token)
             (type-int-keyword? next-token))
        (-> state
            step-next  ;; unsigned
            step-next  ;; int
            (assoc :ast {:type :int :modifiers [:unsigned]}))
        
        ;; Unsigned char
        (and (type-unsigned-keyword? token)
             (type-char-keyword? next-token))
        (-> state
            step-next  ;; unsigned
            step-next  ;; char
            (assoc :ast {:type :char :modifiers [:unsigned]}))
        
        ;; Signed int (явно указанный)
        (and (type-signed-keyword? token)
             (type-int-keyword? next-token))
        (-> state
            step-next  ;; signed
            step-next  ;; int
            (assoc :ast {:type :int :modifiers [:signed]}))
        
        ;; Signed char (явно указанный)
        (and (type-signed-keyword? token)
             (type-char-keyword? next-token))
        (-> state
            step-next  ;; signed
            step-next  ;; char
            (assoc :ast {:type :char :modifiers [:signed]}))
        
        ;; Неявный signed int
        (type-int-keyword? token)
        (-> state
            step-next
            (assoc :ast {:type :int :modifiers []}))
        
        ;; Неявный signed char
        (type-char-keyword? token)
        (-> state
            step-next
            (assoc :ast {:type :char :modifiers []}))
        
        ;; Void
        (type-void-keyword? token)
        (-> state
            step-next
            (assoc :ast {:type :void :modifiers []}))
        
        :else 
        (do 
          (log/error "Неизвестный тип:" (when token (:type token)) (when token (:value token)))
          state)))))

(defn parse-variable-declarations
  "Парсит список объявлений переменных через запятую.
   Возвращает вектор деклараций и новое состояние."
  [type-state]
  (log/trace "Entering parse-variable-declarations with state:" 
             {:position (:position type-state)
              :current-token (when-let [t (current-token type-state)] 
                             {:type (:type t) :value (:value t)})
              :type-ast (:ast type-state)})
  (let [type-ast (:ast type-state)]
    (log/debug "parse-variable-declarations: Starting with type:" type-ast)
    (loop [current-state type-state
           declarations []]
      (let [name-token (current-token current-state)
            next-token (next-token current-state)]
        (log/debug "parse-variable-declarations: Processing tokens:" 
                  "\n  - Name token:" (when name-token {:type (:type name-token) :value (:value name-token)})
                  "\n  - Next token:" (when next-token {:type (:type next-token) :value (:value next-token)}))
        
        (if (identifier? name-token)
          (let [var-decl (nodes/->VariableDeclaration 
                          (:type type-ast)
                          (:value name-token))
                next-state (step-next current-state)
                separator-token (current-token next-state)]
            
            (log/debug "parse-variable-declarations: Created declaration:" var-decl)
            (log/debug "parse-variable-declarations: Separator token:" 
                      (when separator-token {:type (:type separator-token) :value (:value separator-token)}))
            
            (cond
              ;; Точка с запятой - конец списка
              (semicolon? separator-token)
              (do
                (log/debug "parse-variable-declarations: End of declarations (semicolon)")
                [(conj declarations var-decl) (step-next next-state)])
              
              ;; Запятая - продолжаем парсить
              (comma? separator-token)
              (do
                (log/debug "parse-variable-declarations: More declarations (comma)")
                (recur (step-next next-state) (conj declarations var-decl)))
              
              :else
              (do
                (log/error "parse-variable-declarations: Invalid separator after variable:" 
                          (when separator-token {:type (:type separator-token) :value (:value separator-token)}))
                (handle-error current-state 
                            {:expected "Expected comma or semicolon after variable declaration"
                             :found (when separator-token {:type (:type separator-token)
                                                        :value (:value separator-token)})}))))
          
          ;; Не идентификатор
          (do
            (log/error "parse-variable-declarations: Expected identifier, got:" 
                      (when name-token {:type (:type name-token) :value (:value name-token)}))
            (handle-error current-state 
                        {:expected "Expected identifier for variable declaration"
                         :found (when name-token {:type (:type name-token)
                                               :value (:value name-token)})})))))))

(defn is-main-ahead?
  "Проверяет, является ли следующий токен (или через один для signed/unsigned) main"
  [state]
  (let [token (current-token state)
        next-token (next-token state)
        next-next-token (next-next-token state)]
    (cond
      ;; Для signed/unsigned проверяем через один токен
      (or (type-signed-keyword? token)
          (type-unsigned-keyword? token))
      (type-main-keyword? next-next-token)
      
      ;; Для обычных типов проверяем следующий токен
      :else 
      (type-main-keyword? next-token))))

(defn parse-statement
  "Универсальная функция для разбора различных операторов, 
   включая объявления функций, переменных, операторов возврата 
   и специфичных для C51 конструкций."
  [state]
  (log/trace "Entering parse-statement with state:" 
             {:position (:position state)
              :current-token (when-let [t (current-token state)] 
                             {:type (:type t) :value (:value t)})})
  (let [token (current-token state)]
    (log/trace "Entering parse-statement. Current token:" 
               (when token {:type (:type token) :value (:value token)})
               "Position:" (:position state))
    (let [next-token (next-token state)
          next-next-token (next-next-token state)]
      (log/debug "parse-statement details. Next token:" 
                 (when next-token 
                   {:type (:type next-token) 
                    :value (:value next-token)})
                 "Next-next token:" (when next-next-token 
                                    {:type (:type next-next-token) 
                                     :value (:value next-next-token)}))
      
      (cond
        ;; Объявление переменной или функции (с поддержкой signed/unsigned)
        (or (type-void-keyword? token)
            (type-int-keyword? token)
            (type-char-keyword? token)
            (type-unsigned-keyword? token)
            (type-signed-keyword? token))
        (if (is-main-ahead? state)
          ;; Если впереди main - парсим его напрямую
          (let [type-state (parse-type state)]
            (parse-main-declaration type-state))
          ;; Иначе обычный парсинг
          (let [type-state (parse-type state)
                name-token (current-token type-state)]
            (log/debug "parse-statement: After type parsing. Name token:" 
                       (when name-token 
                         {:type (:type name-token) 
                          :value (:value name-token)})
                       "Type AST:" (:ast type-state))
            (cond
              ;; Объявление переменной (включая список через запятую)
              (identifier? name-token)
              (let [[declarations final-state] (parse-variable-declarations type-state)]
                (if (seq declarations)
                  (assoc final-state :ast (first declarations))
                  (handle-error type-state)))
              
              ;; Объявление функции
              (open-round-bracket? (next-token type-state))
              (parse-function-declaration type-state)
              
              :else 
              (do 
                (log/error "Ошибка в объявлении переменной или функции:" 
                          "\n  - Имя:" (when name-token (:value name-token))
                          "\n  - Тип токена:" (when name-token (:type name-token))
                          "\n  - Следующий токен:" (when-let [nt (next-token type-state)] 
                                                   {:type (:type nt) :value (:value nt)}))
                (handle-error type-state)))))
  
        ;; Оператор присваивания (идентификатор = выражение;)
        (and (identifier? token)
             (equal? next-token))
        (let [[new-state ast] (parse-assignment state)]
          (if ast
            (assoc new-state :ast ast)
            (handle-error state)))
        
        ;; Другие операторы (return, if, etc.) остаются без изменений
        :else state))))

(defn parse-binary-expression
  "Парсит бинарное выражение с учетом приоритета операторов"
  [state min-precedence]
  (let [[current-state result] (parse-primary-expression state)]
    (if result
      (loop [current-state current-state
             result result]
        (let [op-token (current-token current-state)]
          (if (and op-token
                   (or (plus? op-token)
                       (minus? op-token)
                       (multiply? op-token)
                       (divide? op-token)
                       (modulo? op-token))
                   (>= (get operator-precedence (:value op-token)) min-precedence))
            (let [op-precedence (get operator-precedence (:value op-token))
                  next-min-precedence (inc op-precedence)
                  after-op (step-next current-state)
                  [rhs-state rhs] (parse-binary-expression after-op next-min-precedence)]
              (if rhs
                (recur rhs-state
                       (nodes/->BinaryExpression (:value op-token) result rhs))
                [current-state result]))
            [current-state result])))
      ;; Если первичное выражение не удалось разобрать
      [state nil])))

(defn parse-primary-expression
  "Парсит первичное выражение (числа, идентификаторы, выражения в скобках)"
  [state]
  (let [token (current-token state)]
    (log/trace "Entering parse-expression. Current token:" 
               (when token {:type (:type token) :value (:value token)})
               "Position:" (:position state))
    (cond
      ;; Открывающая скобка - выражение в скобках
      (open-round-bracket? token)
      (let [[after-expr-state expr] (parse-expression (step-next state))
            close-token (current-token after-expr-state)]
        (if (close-round-bracket? close-token)
          [(step-next after-expr-state) expr]
          (handle-error after-expr-state
                     {:context "Missing closing parenthesis"})))
      
      ;; Числовой литерал
      (int-number? token)
      [(step-next state) (nodes/->Literal :int (:value token))]
      
      ;; Идентификатор
      (identifier? token)
      [(step-next state) (nodes/->Identifier (:value token))]
      
      ;; Выражение в скобках
      :else
      [state nil])))

(defn parse-expression
  "Парсит выражение, включая арифметические операции"
  [state]
  (parse-binary-expression state 0))

(defn parse-assignment
  "Парсит оператор присваивания.
   Поддерживает простые присваивания вида: identifier = expression;"
  [state]
  (log/trace "Entering parse-assignment with state:" 
             {:position (:position state)
              :current-token (when-let [t (current-token state)] 
                             {:type (:type t) :value (:value t)})})
  (let [left-token (current-token state)]
    (if (identifier? left-token)
      (let [left (nodes/->Identifier (:value left-token))
            after-left (step-next state)
            op-token (current-token after-left)]
        (if (equal? op-token)
          (let [after-op (step-next after-left)
                [after-right right-expr] (parse-expression after-op)]
            (if right-expr
              (let [semicolon-token (current-token after-right)]
                (if (semicolon? semicolon-token)
                  [(step-next after-right) 
                   (nodes/->Assignment left right-expr)]
                  (handle-error after-right
                              {:context "Missing semicolon after assignment"
                               :found semicolon-token
                               :expected ";"})))
              (handle-error after-op
                          {:context "Invalid expression in assignment"
                           :found (current-token after-op)})))
          (handle-error after-left
                      {:context "Expected '=' in assignment"
                       :found op-token})))
      (handle-error state
                  {:context "Expected identifier in assignment"
                   :found left-token}))))

(defn parse-block
  "Парсит блок кода, заключенный в фигурные скобки.
   Возвращает [new-state statements]"
  [state]
  (log/trace "Entering parse-block with state:" 
             {:position (:position state)
              :current-token (when-let [t (current-token state)] 
                             {:type (:type t) :value (:value t)})})
  (let [open-token (current-token state)]
    (if (open-curly-bracket? open-token)
      (loop [current-state (step-next state)
             statements []]
        (let [token (current-token current-state)]
          (cond
            ;; Закрывающая скобка - конец блока
            (close-curly-bracket? token)
            [(step-next current-state) (nodes/->Block statements)]
            
            ;; Конец файла - ошибка
            (nil? token)
            (handle-error state {:context "Unexpected end of file in block"})
            
            ;; Парсим следующий оператор
            :else
            (let [stmt-state (parse-statement current-state)]
              (if (:ast stmt-state)
                (recur stmt-state (conj statements (:ast stmt-state)))
                (handle-error current-state 
                            {:context "Failed to parse statement in block"}))))))
      (handle-error state {:context "Expected '{' at start of block"}))))

(defn parse-return-statement
  "Парсит оператор return.
   Возвращает [new-state ast]"
  [state]
  (let [return-token (current-token state)]
    (if (type-return-keyword? return-token)
      (let [expr-state (step-next state)
            [after-expr-state expr] (parse-expression expr-state)
            semicolon-token (current-token after-expr-state)]
        (if (semicolon? semicolon-token)
          [(step-next after-expr-state) 
           (nodes/->ReturnStatement expr)]
          (handle-error after-expr-state
                      {:context "Missing semicolon after return statement"})))
      (handle-error state
                   {:context "Expected 'return' keyword"}))))

(defn parse-for-loop
  "Парсит цикл for.
   Возвращает [new-state ast]"
  [state]
  (let [for-token (current-token state)]
    (if (type-for-keyword? for-token)
      (let [open-paren-state (step-next state)
            open-paren-token (current-token open-paren-state)]
        (if (open-round-bracket? open-paren-token)
          (let [[init-state init] (parse-expression (step-next open-paren-state))
                semicolon1-token (current-token init-state)]
            (if (semicolon? semicolon1-token)
              (let [[end-state end] (parse-expression (step-next init-state))
                    semicolon2-token (current-token end-state)]
                (if (semicolon? semicolon2-token)
                  (let [[step-state step] (parse-expression (step-next end-state))
                        close-paren-token (current-token step-state)]
                    (if (close-round-bracket? close-paren-token)
                      (let [body-state (parse-block (step-next step-state))]
                        (if (:ast body-state)
                          [body-state (nodes/->ForLoop init end step (:ast body-state))]
                          (handle-error step-state
                                     {:context "Failed to parse for loop body"})))
                      (handle-error step-state
                                 {:context "Missing closing parenthesis in for loop"})))
                  (handle-error end-state
                             {:context "Missing semicolon after end expression in for loop"})))
              (handle-error init-state
                         {:context "Missing semicolon after init expression in for loop"})))
          (handle-error open-paren-state
                     {:context "Missing opening parenthesis in for loop"})))
      (handle-error state
                   {:context "Expected 'for' keyword"}))))

(defn parse-while-loop
  "Парсит цикл while.
   Возвращает [new-state ast]"
  [state]
  (let [while-token (current-token state)]
    (if (type-while-keyword? while-token)
      (let [open-paren-state (step-next state)
            open-paren-token (current-token open-paren-state)]
        (if (open-round-bracket? open-paren-token)
          (let [[cond-state condition] (parse-expression (step-next open-paren-state))
                close-paren-token (current-token cond-state)]
            (if (close-round-bracket? close-paren-token)
              (let [body-state (parse-block (step-next cond-state))]
                (if (:ast body-state)
                  [body-state (nodes/->WhileLoop condition (:ast body-state))]
                  (handle-error cond-state
                             {:context "Failed to parse while loop body"})))
              (handle-error cond-state
                         {:context "Missing closing parenthesis in while loop"})))
          (handle-error open-paren-state
                     {:context "Missing opening parenthesis in while loop"})))
      (handle-error state
                   {:context "Expected 'while' keyword"}))))

(defn parse-interrupt-declaration
  "Парсит объявление прерывания.
   Возвращает [new-state ast]"
  [state]
  (let [interrupt-token (current-token state)]
    (if (interrupt-keyword? interrupt-token)
      (let [number-state (step-next state)
            number-token (current-token number-state)]
        (if (int-number? number-token)
          (let [func-state (step-next number-state)
                func-token (current-token func-state)]
            (if (identifier? func-token)
              [(step-next func-state) 
               (nodes/->InterruptDeclaration 
                 (Integer/parseInt (:value number-token))
                 (:value func-token))]
              (handle-error func-state
                         {:context "Expected function name in interrupt declaration"})))
          (handle-error number-state
                     {:context "Expected interrupt number"})))
      (handle-error state
                   {:context "Expected 'interrupt' keyword"}))))

(defn parse-using-declaration
  "Парсит объявление using.
   Возвращает [new-state ast]"
  [state]
  (let [using-token (current-token state)]
    (if (using-keyword? using-token)
      (let [name-state (step-next state)
            name-token (current-token name-state)]
        (if (identifier? name-token)
          (let [open-paren-state (step-next name-state)
                open-paren-token (current-token open-paren-state)]
            (if (open-round-bracket? open-paren-token)
              (loop [current-state (step-next open-paren-state)
                     vars []]
                (let [var-token (current-token current-state)]
                  (cond
                    (identifier? var-token)
                    (let [next-state (step-next current-state)
                          next-token (current-token next-state)]
                      (cond
                        (comma? next-token)
                        (recur (step-next next-state)
                              (conj vars (:value var-token)))
                        
                        (close-round-bracket? next-token)
                        [(step-next next-state)
                         (nodes/->UsingDeclaration 
                           (:value name-token)
                           (conj vars (:value var-token)))]
                        
                        :else
                        (handle-error next-state
                                   {:context "Expected comma or closing parenthesis in using declaration"})))
                    
                    (close-round-bracket? var-token)
                    [(step-next current-state)
                     (nodes/->UsingDeclaration (:value name-token) vars)]
                    
                    :else
                    (handle-error current-state
                               {:context "Expected identifier or closing parenthesis in using declaration"}))))
              (handle-error open-paren-state
                         {:context "Missing opening parenthesis in using declaration"})))
          (handle-error name-state
                     {:context "Expected identifier after using keyword"})))
      (handle-error state
                   {:context "Expected 'using' keyword"}))))