(ns c51cc.parser
  (:require [c51cc.logger :as log]
            [c51cc.ast.nodes :as nodes]
            [c51cc.config :refer [DEBUG]]))

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
;; Оператор присваивания содержит левую и правую часть и тип операции
(defrecord Assignment [left right operator])
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
(defrecord PointerType [base-type])
;; Массив содержит базовый тип и размеры
(defrecord ArrayType [base-type dimensions])

;;Цикл for содержит начальное значение, конечное значение, шаг, тело
(defrecord ForLoop [init end step body])
;;Цикл while содержит условие, тело
(defrecord WhileLoop [condition body])
;;Цикл do-while содержит тело, условие
(defrecord DoWhileLoop [body condition])

;; Объявление прерывания содержит номер и функцию
(defrecord InterruptDeclaration [number func])
;; Объявление using содержит имя и список переменных
(defrecord UsingDeclaration [name vars])

;;==================================================
;; Парсер
;;==================================================

;; Предварительные объявления всех функций
(declare 
          ;; Работа с ветором токенов
          current-token               ;;	 Возвращает текущий токен
          next-token                  ;;	 Возвращает следующий токен без перемещения позиции
          next-next-token             ;;	 Возвращает дважды следующий токен без перемещения позиции
          step-back                   ;;	 Возвращается на один токен назад
          step-next                   ;;	 Перемещается на один токен вперед
          expect-token               	;;	 Проверяет, соответствует ли текущий токен ожидаемому типу и значению, и переходит к следующему токену.
          handle-error 	              ;;	 Обработка ошибок синтаксического анализа, которая может включать вывод сообщений об ошибках и восстановление состояния парсера.
          
          parse-function-declaration  ;;	 Обрабатывает объявления функций, включая параметры, возвращаемый тип и тело функции.
          parse-function-params   	  ;;	 Обрабатывает список параметров функции, включая их типы и имена.
          parse-main-declaration      ;;   Обрабатывает объявление функции main

          format-declaration-error    ;;	 Ошибка объявления переменной.
          parse-return-statement      ;; 
          parse-variable-declarations
          
          
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


(defn current-token
  "Функция возвращает текущий токен
  
  Аргумент:
  - `state` : Состояние (в рамках FSM)
  
  Возвращает:
  - token : Токен состояния."
  [state]
  (when DEBUG
    (log/paranoic "Entering current-token:" state ))
  (when state  ;; Guard against nil state
    (let [tokens (:tokens state)
          position (:position state)]
      (when (and tokens ;; Guard against nil tokens
                 (not-empty tokens)
                 (number? position)
                 (< position (count tokens)))
        (nth tokens position)
        ;; Функция nth используется для получения текущего токена 
        ;; из вектора токенов tokens по индексу position. 
        ;; Это позволяет парсеру получить доступ к конкретному токену, 
        ;; который он должен обрабатывать в данный момент
        ))))

(defn next-token
  "Функция возвращает следующий токен от заданного
  Если токена не существует -- возвращается nil"
  [state]
  (when DEBUG
    (log/paranoic "Entering next-token:" state ))
  (let [tokens (:tokens state)
        position (:position state)
        next-pos (inc position)]
    (if (and (< next-pos (count tokens))
             (not-empty tokens))
      (nth tokens next-pos)
      nil)))

(defn next-next-token
  "Функция возвращает дважды следующий токен от заданного
  Если токена не существует -- возвращается nil"
  [state]
  (when DEBUG
    (log/paranoic "Entering next-next-token:" state ))
  (let [tokens (:tokens state)
        position (:position state)
        next-next-pos (inc (inc position))] ;; Дважды следующий
    (if (and (< next-next-pos (count tokens))
             (not-empty tokens))
      (nth tokens next-next-pos)
      nil)))

(defn step-back 
  "Возвращаемся на шаг назад по вектору токентов"
  [state]
  (when DEBUG
    (log/paranoic "Entering step-back:" state ))
  (let [tokens (:tokens state)
        position (:position state)
        prev-pos (dec position)]
    (when (and (< prev-pos (count tokens))
               (not-empty tokens))
      (assoc state :position prev-pos)
      ;; assoc: Эта функция создает новую ассоциативную структуру, 
      ;; добавляя или обновляя ключ-значение. 
      ;; В данном случае она обновляет значение ключа :position в состоянии парсера.
      )))

(defn step-next 
  "Шаг вперед по вектору токенов"
  [state]
  (when DEBUG
    (log/paranoic "Entering step-next:" state ))
  (let [tokens (:tokens state)
        position (:position state)
        next-pos (inc position)]
    (if (and (< next-pos (count tokens))
             (not-empty tokens))
      (assoc state :position next-pos)
      nil)))  ;; Return nil to indicate we've reached the end

;; Ожидаемый токен с ожидаемым типом и значением
(defn expect-token
  "Функция проверят ожидаемый токен.
  
  Входные заняения:
  - state : проверяемый токен
  - expected-type  : ожидаемый тип токена
  - expected-value : ожидаемое значение токена
  
  Если токен соответствует, переходим к следующему токену."

  [state expected-type expected-value]
  (when DEBUG
    (log/paranoic "Entering expect-token:" (current-token state) expected-type expected-value))
  ;; Получаем текущий токен из состояния парсера
  (let [token (current-token state)]
    ;; Проверяем, соответствует ли тип и значение токена ожидаемым
    (when (and (= (:type token) expected-type)
               (= (:value token) expected-value))
      ;; Если токен соответствует, переходим к следующему токену
      (step-next state))))
; ===================  MISC ==================================
(defn print-token
  "Печать токена [token]
  Уровень логирования -- debug"
  [token]
  (when (and token DEBUG)
    (log/debug (format "Type: %-20s Value: %s" 
                    (name (:type token))
                    (:value token)))))


; =========================================================

;; ==========================================================
;; Функции-предикаты для проверки токенов
;; ==========================================================

;; Типы переменных
(defn type-void-keyword?     [token] (when DEBUG (log/paranoic "type-void-keyword?"     token)) (= :void-type-keyword                 (:type token)))
(defn type-int-keyword?      [token] (when DEBUG (log/paranoic "type-int-keyword?"      token)) (= :int-type-keyword                  (:type token)))
(defn type-char-keyword?     [token] (when DEBUG (log/paranoic "type-char-keyword?"     token)) (= :char-type-keyword                 (:type token)))
(defn type-signed-keyword?   [token] (when DEBUG (log/paranoic "type-signed-keyword?"   token)) (= :signed-type-keyword               (:type token)))
(defn type-unsigned-keyword? [token] (when DEBUG (log/paranoic "type-unsigned-keyword?" token)) (= :unsigned-type-keyword             (:type token)))

;; Управляющие конструкции
(defn type-if-keyword?       [token] (when DEBUG (log/paranoic "type-if-keyword?"     token)) (= :if-control-keyword                (:type token)))
(defn type-else-keyword?     [token] (when DEBUG (log/paranoic "type-else-keyword?"   token)) (= :else-control-keyword              (:type token)))
(defn type-while-keyword?    [token] (when DEBUG (log/paranoic "type-while-keyword?"  token)) (= :while-control-keyword             (:type token)))
(defn type-for-keyword?      [token] (when DEBUG (log/paranoic "type-for-keyword?"    token)) (= :for-control-keyword               (:type token)))
(defn type-return-keyword?   [token] (when DEBUG (log/paranoic "type-return-keyword?" token)) (= :return-control-keyword            (:type token)))

;; Ключевое слово main
(defn type-main-keyword?     [token] (when DEBUG (log/paranoic "type-main-keyword?"   token)) (= :main-keyword                      (:type token)))

;; Специальные ключевые слова микроконтроллера
(defn interrupt-keyword?     [token] (when DEBUG (log/paranoic "interrupt-keyword?" token)) (= :interrupt-c51-keyword             (:type token)))
(defn sfr-keyword?           [token] (when DEBUG (log/paranoic "sfr-keyword?"       token)) (= :sfr-c51-keyword                   (:type token)))
(defn sbit-keyword?          [token] (when DEBUG (log/paranoic "sbit-keyword?"      token)) (= :sbit-c51-keyword                  (:type token)))
(defn using-keyword?         [token] (when DEBUG (log/paranoic "using-keyword?"     token)) (= :using-c51-keyword                 (:type token)))

;; Скобки
(defn open-round-bracket?    [token] (when DEBUG (log/paranoic "open-round-bracket?"   token)) (= :open-round-bracket                (:type token)))
(defn close-round-bracket?   [token] (when DEBUG (log/paranoic "close-round-bracket?"  token)) (= :close-round-bracket               (:type token)))
(defn open-curly-bracket?    [token] (when DEBUG (log/paranoic "open-curly-bracket?"   token)) (= :open-curly-bracket                (:type token)))
(defn close-curly-bracket?   [token] (when DEBUG (log/paranoic "close-curly-bracket?"  token)) (= :close-curly-bracket               (:type token)))
(defn open-square-bracket?   [token] (when DEBUG (log/paranoic "open-square-bracket?"  token)) (= :open-square-bracket               (:type token)))
(defn close-square-bracket?  [token] (when DEBUG (log/paranoic "close-square-bracket?" token)) (= :close-square-bracket              (:type token)))

;; Операторы сравнения
(defn greater?               [token] (when DEBUG (log/paranoic "greater?"       token)) (= :greater-comparison-operator       (:type token)))
(defn less?                  [token] (when DEBUG (log/paranoic "less?"          token)) (= :less-comparison-operator          (:type token)))
(defn greater-equal?         [token] (when DEBUG (log/paranoic "greater-equal?" token)) (= :greater-equal-comparison-operator (:type token)))
(defn less-equal?            [token] (when DEBUG (log/paranoic "less-equal?"    token)) (= :less-equal-comparison-operator    (:type token)))
(defn not-equal?             [token] (when DEBUG (log/paranoic "not-equal?"     token)) (= :not-equal-comparison-operator     (:type token)))

;; Операторы присваивания
(defn equal?                 [token] (when DEBUG (log/paranoic "equal?"         token)) (= :equal-assignment-operator         (:type token)))
(defn and-equal?             [token] (when DEBUG (log/paranoic "and-equal?"     token)) (= :and-equal-assignment-operator     (:type token)))
(defn or-equal?              [token] (when DEBUG (log/paranoic "or-equal?"      token)) (= :or-equal-assignment-operator      (:type token)))
(defn xor-equal?             [token] (when DEBUG (log/paranoic "xor-equal?"     token)) (= :xor-equal-assignment-operator     (:type token)))

;; Битовые операторы
(defn and-bitwise?           [token] (when DEBUG (log/paranoic "and-bitwise?"     token)) (= :and-bitwise-operator              (:type token)))
(defn or-bitwise?            [token] (when DEBUG (log/paranoic "or-bitwise?"      token)) (= :or-bitwise-operator               (:type token)))
(defn xor-bitwise?           [token] (when DEBUG (log/paranoic "xor-bitwise?"     token)) (= :xor-bitwise-operator              (:type token)))
(defn not-bitwise?           [token] (when DEBUG (log/paranoic "not-bitwise?"     token)) (= :not-bitwise-operator              (:type token)))

;; Разделители
(defn semicolon?             [token] (when DEBUG (log/paranoic "semicolon?"     token)) (= :semicolon-separator               (:type token)))
(defn comma?                 [token] (when DEBUG (log/paranoic "comma?"         token))(= :comma-separator                   (:type token)))
(defn dot?                   [token] (when DEBUG (log/paranoic "dot?"           token))(= :dot-separator                     (:type token)))
(defn colon?                 [token] (when DEBUG (log/paranoic "colon?"         token))(= :colon-separator                   (:type token)))

;; Арифметические операторы
(defn plus?                  [token] (when DEBUG (log/paranoic "plus?"       token)) (= :plus-math-operator                (:type token)))
(defn minus?                 [token] (when DEBUG (log/paranoic "minus?"      token)) (= :minus-math-operator               (:type token)))
(defn multiply?              [token] (when DEBUG (log/paranoic "multiply?"   token)) (= :multiply-math-operator            (:type token)))
(defn divide?                [token] (when DEBUG (log/paranoic "divide?"     token)) (= :divide-math-operator              (:type token)))
(defn modulo?                [token] (when DEBUG (log/paranoic "modulo?"     token)) (= :modulo-math-operator              (:type token)))


;; Инкремент и декремент
(defn increment?             [token] (when DEBUG (log/paranoic "increment?"     token)) (= :increment-operator                (:type token)))
(defn decrement?             [token] (when DEBUG (log/paranoic "decrement?"     token)) (= :decrement-operator                (:type token)))

;; Логические операторы
(defn or-logical?            [token] (when DEBUG (log/paranoic "or-logical?"        token)) (= :or-logical-operator               (:type token)))
(defn and-logical?           [token] (when DEBUG (log/paranoic "and-logical?"       token)) (= :and-logical-operator              (:type token)))
(defn equal-logical?         [token] (when DEBUG (log/paranoic "equal-logical?"     token)) (= :equal-logical-operator            (:type token)))
(defn not-equal-logical?     [token] (when DEBUG (log/paranoic "not-equal-logical?" token)) (= :not-equal-logical-operator        (:type token)))
(defn not-logical?           [token] (when DEBUG (log/paranoic "not-logical?"       token)) (= :not-logical-operator              (:type token)))

;; Числа
(defn int-number?            [token] (when DEBUG (log/paranoic "int-number?"       token)) (= :int-number                        (:type token)))
(defn hex-number?            [token] (when DEBUG (log/paranoic "hex-number?"       token)) (= :hex-number                        (:type token)))

;; Идентификатор
(defn identifier?            [token] (when DEBUG (log/paranoic "identifier?"       token)) (= :identifier                        (:type token)))

;; Приоритет операций
(def operator-precedence
  {
   "="   1,           ;; присваивание
   "+="  2, "-="  2,  ;; сложение и вычитание
   "||"  3,           ;; логическое ИЛИ
   "&&"  4,           ;; логическое И
   "|"   5, "|="  5,  ;; побитовое ИЛИ
   "^"   6, "^="  6,  ;; побитовое исключающее ИЛИ
   "&"   6, "&="  6,  ;; побитовое И
   "=="  7, "!="  7,  ;; сравнение
   "<="  7, ">="  7,  ;; сравнение
   "<"   7, ">"   7,  ;; сравнение
   "+"   8, "-"   8,  ;; сложение и вычитание
   "*"   9, "/"   9,  ;; умножение и деление
   "%"  10,           ;; остаток от деления
   "++" 11, "--" 11   ;; инкремент и декремент
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
    ;; см @doc/loop.md
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
            (step-next current-state))))))

(defn parser 
  "Алиас к parse-program.
  см (doc parser/parse-program)"
  [tokens]
  (parse-program tokens))

(defn parse-program
  "Основной парсер программы.
   Принимает вектор токенов и возвращает AST программы."
  [tokens]
  (log/trace "Entering parse-program with tokens count:" (count tokens))
  (let [initial-state (->ParserState tokens 0) ;; (defrecord ParserState [tokens position])
        fsm-state (atom {:stage 
                         :start 
                         :declarations []})]
    (loop [current-state initial-state]
  
    (let [token (current-token current-state)]
          ;; next-token (next-token current-state)
          ;; next-next-token (next-next-token current-state)]
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
                    
          ;; Тип данных - начало новой декларации
          (or (type-void-keyword? token)
              (type-int-keyword? token)
              (type-char-keyword? token)
              (type-unsigned-keyword? token)
              (type-signed-keyword? token))
          (let [result (parse-variable-declarations current-state)]
            (if (seq (:ast result))
              (do 
                (swap! fsm-state update :declarations conj (:ast result))
                (recur (:state result)))
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
          ;; for
          ;; while
          ;; do-while
          
          ;; Пропускаем все остальные токены
          :else
          (recur (step-next current-state)))))))

(defn parse-function-params
  "Парсит параметры функции"
  [state]
  (let [token (current-token state)]
    (cond 
      ;; Если текущий токен - открывающая скобка, делаем шаг вперед
      (open-round-bracket? token)
      (let [new-state (step-next state)]
        (parse-function-params new-state))

      ;; Если void внутри скобок
      (type-void-keyword? token)
      (let [next-state (step-next state)
            close-token (current-token next-state)]
        (if (close-round-bracket? close-token)
          {:state (step-next next-state)
           :ast [{:type :void :name nil}]}
          (handle-error next-state 
                        {:context "Ожидается закрывающая скобка после void"})))

      ;; Если закрывающая скобка сразу - тоже пустые параметры
      (close-round-bracket? token)
      {:state (step-next state)
       :ast []}

      ;; Если токен не void и не закрывающая скобка
      :else
      (let [type-result (parse-type state)]
        (if (:ast type-result)
          (let [name-token (current-token (:state type-result))]
            (if (identifier? name-token)
              (let [param {:type (:ast type-result) :name (:value name-token)}
                    next-state (step-next (:state type-result))
                    next-token (current-token next-state)]
                (cond 
                  ;; Запятая - продолжаем парсить параметры
                  (comma? next-token)
                  (let [next-params (parse-function-params (step-next next-state))]
                    {:state (:state next-params)
                     :ast (cons param (:ast next-params))})
                  
                  ;; Закрывающая скобка - конец списка параметров
                  (close-round-bracket? next-token)
                  {:state (step-next next-state)
                   :ast [param]}
                  
                  :else
                  (handle-error next-state 
                              {:context "Неожиданный токен при парсинге параметров"})))
              (handle-error (:state type-result)
                          {:context "Ожидается имя параметра"})))
          (handle-error state
                      {:context "Ожидается тип параметра"}))))))

(defn parse-main-declaration
  [state]
  (let [type-result (parse-type state)]
    (if (and (:ast type-result) 
             (= (:type (:ast type-result)) :void))
      (let [main-token (current-token (:state type-result))]
        (if (type-main-keyword? main-token)
          (let [params-result (parse-function-params (step-next (:state type-result)))]
            (if (:ast params-result)
              (let [body-result (parse-block (:state params-result))]
                (if (:ast body-result)
                  {:state (:state body-result)
                   :ast (nodes/->FunctionDeclaration 
                         :void
                         "main"
                         (:ast params-result)
                         nil  ;; interrupt
                         nil  ;; using
                         (:ast body-result))}
                  (handle-error (:state params-result)
                            {:context "Failed to parse main function body"})))
              (handle-error (:state type-result)
                        {:context "Failed to parse main function parameters"})))
          (handle-error (:state type-result)
                    {:context "Expected 'main' keyword"})))
      (handle-error state
                  {:context "Main function must return void"}))))

(defn parse-function-declaration
  [state]
  (let [type-result (parse-type state)]
    (if (nil? (:ast type-result))
      (handle-error state {:context "Invalid return type"})
      (let [name-token (current-token (:state type-result))]
        (if (identifier? name-token)
          (let [params-state (step-next (:state type-result))
                params-result (parse-function-params params-state)]
            (if (:ast params-result)
              (let [body-result (parse-block (:state params-result))]
                (if (:ast body-result)
                  {:state (:state body-result)
                   :ast (nodes/->FunctionDeclaration 
                         (:ast type-result)
                         (:value name-token)
                         (:ast params-result)
                         nil  ;; interrupt
                         nil  ;; using
                         (:ast body-result))}
                  (handle-error (:state params-result)
                            {:context "Failed to parse function body"})))
              (handle-error params-state
                          {:context "Failed to parse function parameters"})))
          (handle-error (:state type-result)
                      {:context "Expected function name"}))))))


(defn parse-sfr-declaration
  "Парсер объявления SFR (Special Function Register) с расширенной логикой проверки.
   
   Последовательность токенов:
   [sfr][identifier][=][hex-address][;]
   
   Ключевые особенности:
   1. Строгая проверка последовательности токенов
   2. Детальная диагностика синтаксических ошибок
   3. Поддержка объявления SFR с адресацией"
  [state]
  (let [token           (current-token   state)
        next-token      (next-token      state)
        next-next-token (next-next-token state)]

    (log/trace "Проверка последовательности токенов SFR"
               "\n Position       :" (:position state)
               "\n Токены         :" 
               {:token     (when token {:type (:type token) :value (:value token)})
                :next      (when next-token {:type (:type next-token) :value (:value next-token)})
                :next-next (when next-next-token {:type (:type next-next-token) :value (:value next-next-token)})})

    ;; Проверка ключевого слова SFR
    (cond 
      (not (sfr-keyword? token))
      (handle-error state 
                    {:context "Ожидается ключевое слово sfr"
                     :expected "sfr"
                     :found (:value token)})

      ;; Проверка имени SFR
      (not (identifier? next-token))
      (handle-error state 
                    {:context "Отсутствует имя SFR"
                     :expected "Идентификатор"
                     :found (:value next-token)})

      ;; Проверка оператора присваивания
      (not (equal? next-next-token))
      (handle-error state 
                    {:context "Ожидается оператор присваивания адреса"
                     :expected "="
                     :found (:value next-next-token)})

      :else
      (let [name-token     next-token
            address-start-state (step-next (assoc state :current-token next-next-token))
            address-token (current-token address-start-state)]

        (cond
          ;; Проверка адреса (hex-number)
          (not (hex-number? address-token))
          (handle-error address-start-state 
                        {:context "Ожидается шестнадцатеричный адрес SFR"
                         :expected "Hex-адрес"
                         :found (:value address-token)})

          :else
          (let [after-address-state (step-next address-start-state)
                semicolon-token (current-token after-address-state)]

            (cond
              ;; Проверка точки с запятой
              (not (semicolon? semicolon-token))
              (handle-error after-address-state 
                            {:context "Ожидается точка с запятой после объявления SFR"
                             :expected ";"
                             :found (:value semicolon-token)})

              :else
              (let [final-state (step-next after-address-state)]
                (assoc final-state 
                       :ast 
                       (nodes/->SfrDeclaration 
                         (:value name-token)    ;; name
                         (Integer/parseInt (:value address-token) 16))))))))))) ;; address

(defn parse-sbit-declaration
  "Парсер объявления SBIT (Special Bit Register) с расширенной логикой проверки.
   
   Последовательность токенов:
   [sbit][identifier][=][bit-number][;]
   
   Ключевые особенности:
   1. Строгая проверка последовательности токенов
   2. Детальная диагностика синтаксических ошибок
   3. Поддержка объявления SBIT с указанием номера бита"
  [state]
  (let [token           (current-token   state)
        next-token      (next-token      state)
        next-next-token (next-next-token state)]

    (log/trace "Проверка последовательности токенов SBIT"
               "\n Position       :" (:position state)
               "\n Токены         :" 
               {:token     (when token {:type (:type token) :value (:value token)})
                :next      (when next-token {:type (:type next-token) :value (:value next-token)})
                :next-next (when next-next-token {:type (:type next-next-token) :value (:value next-next-token)})})

    ;; Проверка ключевого слова SBIT
    (cond 
      (not (sbit-keyword? token))
      (handle-error state 
                    {:context "Ожидается ключевое слово sbit"
                     :expected "sbit"
                     :found (:value token)})

      ;; Проверка имени SBIT
      (not (identifier? next-token))
      (handle-error state 
                    {:context "Отсутствует имя SBIT"
                     :expected "Идентификатор"
                     :found (:value next-token)})

      ;; Проверка оператора присваивания
      (not (equal? next-next-token))
      (handle-error state 
                    {:context "Ожидается оператор присваивания номера бита"
                     :expected "="
                     :found (:value next-next-token)})

      :else
      (let [name-token     next-token
            bit-start-state (step-next (assoc state :current-token next-next-token))
            bit-token (current-token bit-start-state)]

        (cond
          ;; Проверка номера бита (целое число)
          (not (int-number? bit-token))
          (handle-error bit-start-state 
                        {:context "Ожидается целочисленный номер бита"
                         :expected "Номер бита"
                         :found (:value bit-token)})

          :else
          (let [after-bit-state (step-next bit-start-state)
                semicolon-token (current-token after-bit-state)]

            (cond
              ;; Проверка точки с запятой
              (not (semicolon? semicolon-token))
              (handle-error after-bit-state 
                            {:context "Ожидается точка с запятой после объявления SBIT"
                             :expected ";"
                             :found (:value semicolon-token)})

              :else
              (let [final-state (step-next after-bit-state)]
                (assoc final-state 
                       :ast 
                       (nodes/->SbitDeclaration 
                         (:value name-token)    ;; name
                         (Integer/parseInt (:value bit-token)))))))))))) ;; bit

(defn parse-type
  "Обрабатывает различные типы данных"
  [state]
  (log/trace "Entering parse-type with state:" 
             {:position (:position state)
              :current-token (when-let [t (current-token state)] 
                             {:type (:type t) :value (:value t)})})
  
  (let [token (current-token state)
        next-token (next-token state)]
    
    (log/trace "Parsing type tokens:" 
               {:current (when token {:type (:type token) :value (:value token)})
                :next    (when next-token {:type (:type next-token) :value (:value next-token)})})
    
    (if (nil? token)
      {:state state :ast nil}
      (cond
        ;; Unsigned int
        (and (type-unsigned-keyword? token)
             (type-int-keyword? next-token))
        {:state (-> state step-next step-next)
         :ast {:type :int :modifiers [:unsigned]}}
        
        ;; Unsigned char
        (and (type-unsigned-keyword? token)
             (type-char-keyword? next-token))
        {:state (-> state step-next step-next)
         :ast {:type :char :modifiers [:unsigned]}}
        
        ;; Signed int (явно указанный)
        (and (type-signed-keyword? token)
             (type-int-keyword? next-token))
        {:state (-> state step-next step-next)
         :ast {:type :int :modifiers [:signed]}}
        
        ;; Signed char (явно указанный)
        (and (type-signed-keyword? token)
             (type-char-keyword? next-token))
        {:state (-> state step-next step-next)
         :ast {:type :char :modifiers [:signed]}}
        
        ;; Неявный signed int
        (type-int-keyword? token)
        {:state (step-next state)
         :ast {:type :int :modifiers [:signed]}}
        
        ;; Неявный signed char
        (type-char-keyword? token)
        {:state (step-next state)
         :ast {:type :char :modifiers [:signed]}}
        
        ;; Void
        (type-void-keyword? token)
        {:state (step-next state)
         :ast {:type :void :modifiers []}}
        
        ;; Неизвестный тип
        :else {:state state :ast nil}))))


(defn parse-variable-declarations
  [state]
  (log/trace "Entering parse-variable-declarations with state:" 
             {:position (:position state)
              :current-token (when-let [t (current-token state)] 
                             {:type (:type t) :value (:value t)})})
  
  (let [type-result (parse-type state)]
    (if (nil? (:ast type-result))
      (handle-error state {:context "Неверный тип в объявлении переменной"})
      (let [{:keys [type modifiers]} (:ast type-result)]
        (loop [current-state (:state type-result)
               declarations []]
          (let [name-token (current-token current-state)]
            (if (identifier? name-token)
              (let [var-decl (nodes/->VariableDeclaration 
                              {:type type :modifiers modifiers}
                              (:value name-token))
                    next-state (step-next current-state)
                    separator-token (current-token next-state)]
                
                (cond
                  (semicolon? separator-token)
                  {:state (step-next next-state)
                   :ast (nodes/->Block (conj declarations var-decl))}
                  
                  (comma? separator-token)
                  (recur (step-next next-state) 
                         (conj declarations var-decl))
                  
                  :else
                  (handle-error next-state 
                              {:context "Ожидается запятая или точка с запятой после объявления переменной"})))
              
              (handle-error current-state 
                          {:context "Ожидается имя переменной"}))))))))


(defn parse-statement
  "Универсальная функция для разбора различных операторов, 
   включая объявления функций, переменных, операторов возврата 
   и специфичных для C51 конструкций."
  [state] 
  (log/trace "Entering parse-statement with state:" 
             "\nPosition:"        (:position state)
             "\nCurrent token:"   (when-let [t (current-token state)]
                                   {:type (:type t) :value (:value t)}))

  (let [token           (current-token   state)
        next-token      (next-token      state)
        next-next-token (next-next-token state)]
    (log/trace "Entering parse-statement with state:" 
               "\nPosition:"        (:position state)
               "\nCurrent token:"   (when token           {:type (:type token)           :value (:value token)})
               "\nNext token:"      (when next-token      {:type (:type next-token)      :value (:value next-token)})
               "\nNext-next token:" (when next-next-token {:type (:type next-next-token) :value (:value next-next-token)}))
      
    (cond
      ;; Если void и main -- это функция main
      (and (type-void-keyword? token)
           (type-main-keyword? next-token))
      (parse-main-declaration state)

      ;; Если это объявление типа
      (or (type-signed-keyword?   token)
          (type-unsigned-keyword? token)
          (type-int-keyword?      token)
          (type-char-keyword?     token)
          (type-void-keyword?     token))
      (let [result (parse-variable-declarations state)]
        (if (seq (:ast result))
          ;; Возвращаем все декларации как блок
          (assoc (:state result) :ast (nodes/->Block (:ast result)))
          (handle-error state {:context "Ошибка в объявлении переменной"})))
              
      ;; Оператор присваивания (идентификатор = выражение;)
      ;; (and (identifier? token)
      ;;      (equal? next-token))
      (identifier? token)
      (parse-assignment state)

      ;; Цикл for
      (type-for-keyword? token)
      (parse-for-loop state)

      ;; Цикл while
      (type-while-keyword? token)
      (parse-while-loop state)

      ;; Оператор return
      (type-return-keyword? token)
      (parse-return-statement state)
              
      ;; Если ничего не подошло
      :else 
      (handle-error state 
                    {:context "Неизвестный оператор"
                     :token token}))))

(defn parse-binary-expression
  "Парсит бинарное выражение с учетом приоритета операторов"
  [state min-precedence]
  (let [primary-result (parse-primary-expression state)]
    (if (:ast primary-result)
      (loop [current-state (:state primary-result)
             result (:ast primary-result)]
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
                  rhs-result (parse-binary-expression after-op next-min-precedence)]
              (if (:ast rhs-result)
                (recur (:state rhs-result)
                       (nodes/->BinaryExpression (:value op-token) result (:ast rhs-result)))
                {:state current-state :ast result})))
      {:state state :ast nil})))))


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
      (let [expr-result (parse-expression (step-next state))
            close-token (current-token (:state expr-result))]
        (if (close-round-bracket? close-token)
          {:state (step-next (:state expr-result)) 
           :ast (:ast expr-result)}
          (handle-error (:state expr-result)
                     {:context "Missing closing parenthesis"})))
      
      ;; Числовой литерал
      (int-number? token)
      {:state (step-next state) 
       :ast (nodes/->Literal :int (:value token))}
      
      ;; Идентификатор
      (identifier? token)
      {:state (step-next state) 
       :ast (nodes/->Identifier (:value token))}
      
      ;; Выражение в скобках
      :else
      {:state state :ast nil})))


(defn parse-expression
  "Парсит выражение, включая арифметические операции"
  [state]
  (parse-binary-expression state 0))

(defn parse-assignment
  "Парсит оператор присваивания.
   Поддерживает простые присваивания вида: identifier = expression;
   и составные присваивания вида: identifier += expression;"
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
        (if (or (equal? op-token) 
                (and-equal? op-token)
                (or-equal? op-token)
                (xor-equal? op-token))
          (let [after-op (step-next after-left)
                expr-result (parse-expression after-op)]
            (log/debug "Expression parsed:"
                      "\n  State:" (:state expr-result)
                      "\n  Expression:" (:ast expr-result))
            (if (and (:state expr-result) (:ast expr-result))
              (let [semicolon-token (current-token (:state expr-result))]
                (if (semicolon? semicolon-token)
                  {:state (step-next (:state expr-result))
                   :ast (nodes/->Assignment 
                         left 
                         (:ast expr-result)
                         (case (:type op-token)
                           :equal-assignment-operator     "="
                           :and-equal-assignment-operator "&="
                           :or-equal-assignment-operator  "|="
                           :xor-equal-assignment-operator "^="
                           "="))}
                  (handle-error (:state expr-result)
                              {:context "Missing semicolon after assignment"
                               :found semicolon-token
                               :expected ";"})))
              (handle-error after-op
                          {:context "Failed to parse right-hand expression"
                           :found (current-token after-op)})))
          (handle-error after-left
                      {:context "Expected assignment operator"
                       :found op-token})))
      {:state state :ast nil})))


(defn parse-block
  "Парсит блок кода, заключенный в фигурные скобки"
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
            {:state (step-next current-state) 
             :ast (nodes/->Block statements)}
            
            ;; Конец файла - возвращаем то, что успели собрать
            (nil? token)
            {:state current-state 
             :ast (nodes/->Block statements)}
            
            ;; Парсим следующий оператор
            :else
            (let [stmt-result (parse-statement current-state)]
              (if-let [ast (:ast stmt-result)]
                ;; Если результат это блок, добавляем его содержимое
                (if (instance? c51cc.ast.nodes.Block ast)
                  (recur (:state stmt-result) 
                         (into statements (:statements ast)))
                  (recur (:state stmt-result) 
                         (conj statements ast)))
                ;; Обработка ошибки
                (if-let [recovered-state (handle-error current-state 
                                                     {:context "Failed to parse statement in block"
                                                      :token token})]
                  (recur recovered-state statements)
                  {:state current-state 
                   :ast (nodes/->Block statements)}))))))
      
      ;; Нет открывающей скобки
      (handle-error state 
                    {:context "Expected '{' at start of block"
                     :token open-token}))))

(defn parse-return-statement
  "Парсит оператор return.
   Возвращает {:state :ast}"
  [state]
  (let [return-token (current-token state)]
    (if (type-return-keyword? return-token)
      (let [expr-state (step-next state)
            expr-result (parse-expression expr-state)  ;; Ожидаем {:state :ast}
            semicolon-token (current-token (:state expr-result))]
        (if (semicolon? semicolon-token)
          {:state (step-next (:state expr-result))
           :ast (nodes/->ReturnStatement (:ast expr-result))}
          (handle-error (:state expr-result)
                      {:context "Missing semicolon after return statement"})))
      (handle-error state
                   {:context "Expected 'return' keyword"}))))

(defn parse-for-loop
  [state]
  (let [for-token (current-token state)]
    (if (type-for-keyword? for-token)
      (let [open-paren-state (step-next state)
            open-paren-token (current-token open-paren-state)]
        (if (open-round-bracket? open-paren-token)
          (let [init-result (parse-expression (step-next open-paren-state))
                semicolon1-token (current-token (:state init-result))]
            (if (semicolon? semicolon1-token)
              (let [end-result (parse-expression (step-next (:state init-result)))
                    semicolon2-token (current-token (:state end-result))]
                (if (semicolon? semicolon2-token)
                  (let [step-result (parse-expression (step-next (:state end-result)))
                        close-paren-token (current-token (:state step-result))]
                    (if (close-round-bracket? close-paren-token)
                      (let [body-result (parse-block (step-next (:state step-result)))]
                        (if (:ast body-result)
                          {:state (:state body-result)
                           :ast (nodes/->ForLoop (:ast init-result) 
                                               (:ast end-result) 
                                               (:ast step-result) 
                                               (:ast body-result))}
                          (handle-error (:state step-result)
                                     {:context "Failed to parse for loop body"})))
                      (handle-error (:state step-result)
                                 {:context "Missing closing parenthesis in for loop"})))
                  (handle-error (:state end-result)
                             {:context "Missing semicolon after end expression in for loop"})))
              (handle-error (:state init-result)
                         {:context "Missing semicolon after init expression in for loop"})))
          (handle-error open-paren-state
                     {:context "Missing opening parenthesis in for loop"})))
      (handle-error state
                   {:context "Expected 'for' keyword"}))))


(defn parse-while-loop
  "Парсит цикл while.
   Возвращает {:state :ast}"
  [state]
  (let [while-token (current-token state)]
    (if (type-while-keyword? while-token)
      (let [open-paren-state (step-next state)
            open-paren-token (current-token open-paren-state)]
        (if (open-round-bracket? open-paren-token)
          (let [cond-result (parse-expression (step-next open-paren-state))
                close-paren-token (current-token (:state cond-result))]
            (if (close-round-bracket? close-paren-token)
              (let [body-result (parse-block (step-next (:state cond-result)))]
                (if (:ast body-result)
                  {:state (:state body-result)
                   :ast (nodes/->WhileLoop (:ast cond-result) (:ast body-result))}
                  (handle-error (:state cond-result)
                             {:context "Failed to parse while loop body"})))
              (handle-error (:state cond-result)
                         {:context "Missing closing parenthesis in while loop"})))
          (handle-error open-paren-state
                     {:context "Missing opening parenthesis in while loop"})))
      (handle-error state
                   {:context "Expected 'while' keyword"}))))

(defn parse-interrupt-declaration
  "Парсит объявление прерывания.
   
   Возвращает:
   - {:state :ast} где:
     - state: новое состояние парсера
     - ast: узел InterruptDeclaration"
  [state]
  (let [interrupt-token (current-token state)]
    (if (interrupt-keyword? interrupt-token)
      (let [number-state (step-next state)
            number-token (current-token number-state)]
        (if (int-number? number-token)
          (let [func-state (step-next number-state)
                func-token (current-token func-state)]
            (if (identifier? func-token)
              {:state (step-next func-state)
               :ast (nodes/->InterruptDeclaration 
                 (Integer/parseInt (:value number-token))
                 (:value func-token))}
              (handle-error func-state
                         {:context "Expected function name in interrupt declaration"})))
          (handle-error number-state
                     {:context "Expected interrupt number"})))
      (handle-error state
                   {:context "Expected 'interrupt' keyword"}))))

(defn parse-using-declaration
  "Парсит объявление using.
   
   Возвращает:
   - {:state :ast} где:
     - state: новое состояние парсера
     - ast: узел UsingDeclaration"
  [state]
  (let [using-token (current-token state)]
    (if (using-keyword? using-token)
      (let [name-state (step-next state)
            name-token (current-token name-state)]
        (if (identifier? name-token)
          (let [open-paren-state (step-next name-state)
                open-paren-token (current-token open-paren-state)]
            (if (open-round-bracket? open-paren-token)
              ;; Переписываем loop/recur часть
              (letfn [(parse-vars [current-state vars]
                       (let [var-token (current-token current-state)]
                         (cond
                           ;; Закрывающая скобка - завершаем
                           (close-round-bracket? var-token)
                           {:state (step-next current-state)
                            :ast (nodes/->UsingDeclaration (:value name-token) vars)}
                           
                           ;; Идентификатор - продолжаем парсинг
                           (identifier? var-token)
                           (let [next-state (step-next current-state)
                                 next-token (current-token next-state)]
                             (cond
                               ;; Запятая - следующая переменная
                               (comma? next-token)
                               (recur (step-next next-state) 
                                     (conj vars (:value var-token)))
                               
                               ;; Закрывающая скобка - завершаем
                               (close-round-bracket? next-token)
                               {:state (step-next next-state)
                                :ast (nodes/->UsingDeclaration 
                                      (:value name-token)
                                      (conj vars (:value var-token)))}
                               
                               ;; Ошибка - неожиданный токен
                               :else
                               (handle-error next-state
                                          {:context "Expected comma or closing parenthesis in using declaration"})))
                           
                           ;; Ошибка - неожиданный токен
                           :else
                           (handle-error current-state
                                      {:context "Expected identifier or closing parenthesis in using declaration"}))))]
                ;; Начинаем парсинг переменных
                (parse-vars (step-next open-paren-state) []))
              
              (handle-error open-paren-state
                         {:context "Missing opening parenthesis in using declaration"})))
          (handle-error name-state
                     {:context "Expected identifier after using keyword"})))
      (handle-error state
                   {:context "Expected 'using' keyword"}))))

(defn format-declaration-error
  "Форматирует ошибку объявления переменной.
   Возвращает строку с ошибкой."
   [token location]
  (log/error (format "Error: Variable declaration '%s' must be at the beginning of the %s. 
           Found at position %d"
          (:value token)
          location
          (:position token))))