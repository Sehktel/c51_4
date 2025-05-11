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
;; Унарное выражение содержит тип оператора, оператор и один операнд
(defrecord UnaryExpr [type operator operand]) 
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

;; Объявление прерывания содержит номер и функцию
(defrecord InterruptDecl [number func])
;; Объявление using содержит имя и список переменных
(defrecord UsingDecl [name vars])

;; Структура содержит имя и поля
;; (defrecord StructType [name fields]) ;; не поддерживается
;; Поле структуры содержит тип и имя
;; (defrecord StructField [type name]) ;; не поддерживается

;;==================================================
;; Парсер
;;==================================================

;; Предварительные объявления всех функций
(declare 
          current-token               ;;	 Возвращает текущий токен
          peek-next-token             ;;	 Возвращает следующий токен без перемещения позиции
          advance                     ;;	 Перемещается к следующему токену.
          expect-token               	;;	 Проверяет, соответствует ли текущий токен ожидаемому типу и значению, и переходит к следующему токену.
          handle-error 	              ;;	 Обработка ошибок синтаксического анализа, которая может включать вывод сообщений об ошибках и восстановление состояния парсера.
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
          parse-function-declaration 	;;	 Обрабатывает объявления функций, включая параметры, возвращаемый тип и тело функции.
          parse-function-params   	  ;;	 Обрабатывает список параметров функции, включая их типы и имена.
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
(def parse-expression 
  "Основной парсер выражений. 
   Абстракция над binary-expression для гибкости и расширяемости."
  parse-binary-expression)


;; ==========================================================
;; Перемещение по вектору токенов
;; ==========================================================

;; Мое текущее состояние -- это токен, на котором я сейчас нахожусь.
;; current-token -- это функция, которая возвращает текущий токен.
;; peek-next-token -- это функция, которая возвращает следующий токен.
;; advance -- это функция, которая переходит к следующему токену.
;; expect-token -- это функция, которая ожидает токен с ожидаемым типом и значением.
;; Получается, что я могу перемещаться по вектору токенов, по мере необходимости.

(defn current-token [state]
  (let [tokens (:tokens state)
        position (:position state)]
    (when (and (< position (count tokens))
               (not-empty tokens))
      (nth tokens position))))

(defn peek-next-token [state]
  (let [tokens (:tokens state)
        position (:position state)]
    (when (and (< (inc position) (count tokens))
               (not-empty tokens))
      (nth tokens (inc position)))))

(defn advance [state]
  (update state :position inc))

;; Ожидаемый токен с ожидаемым типом и значением
(defn expect-token [state expected-type expected-value]
  ;; Получаем текущий токен из состояния парсера
  (let [token (current-token state)]
    ;; Проверяем, соответствует ли тип и значение токена ожидаемым
    (when (and (= (:type token) expected-type)
               (= (:value token) expected-value))
      ;; Если токен соответствует, переходим к следующему токену
      (advance state))))

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
  [state]
  (let [fsm-state (atom :initial)
        sync-tokens #{:semicolon-separator :close-curly-bracket}]
    (loop [current-state state]
      (case @fsm-state
        :initial 
        (do (reset! fsm-state :recovery)
            (recur current-state))
        
        :recovery
        (do (log/error "Parser error at token:" (current-token current-state))
            (reset! fsm-state :sync)
            (recur current-state))
        
        :sync
        (let [token (current-token current-state)]
          (cond
            (nil? token) current-state
            (sync-tokens (:type token)) 
            (do (reset! fsm-state :done)
                (advance current-state))
            :else (recur (advance current-state))))
        
        :done current-state))))

;; (defn parse-array-access
;;   "Парсер доступа к элементам массивов с учетом индексации.
;;    Поддерживает: array[index]"
;;   [state]
;;   (let [fsm-state (atom {:stage :start
;;                          :array nil
;;                          :index nil})]
;;     (loop [current-state state]
;;       (let [token (current-token current-state)]
;;         (case (:stage @fsm-state)
;;           :start
;;           (cond
;;             (open-square-bracket? token)
;;             (do 
;;               (reset! fsm-state 
;;                       {:stage :array-index
;;                        :array (:ast current-state)
;;                        :index nil})
;;               (recur (advance current-state)))
            
;;             :else 
;;             (-> current-state
;;                 (#(assoc % :ast (:array @fsm-state))))
          
;;           :array-index
;;           (let [index-state (parse-expression current-state)  ;; First parse the index expression
;;                 after-index-token (current-token index-state)]
;;             (if (close-square-bracket? after-index-token)     ;; Then check for closing bracket
;;               (-> index-state
;;                   advance  ;; Move past the closing bracket
;;                   (#(assoc % :ast 
;;                       {:type :array-access
;;                        :array (:array @fsm-state)
;;                        :index (:ast index-state)})))
;;               (handle-error 
;;                 (assoc index-state :error "Expected closing square bracket after array index"))))
          
;;           :else 
;;           (handle-error 
;;             (assoc current-state :error "Invalid array access parsing state"))))))))

 ;;  Обрабатывает доступ к элементам массивов, включая индексацию.
(defn parse-array-dimensions   [state] (state)) 
 ;;  Обрабатывает размерности массивов, если они присутствуют в объявлении.
(defn parse-array-type   [state] (state)) 
 ;;  Обрабатывает объявления массивов, включая их размерности.
(defn parse-assignment   [state] (state))
;; @doc/Parsing-Flow/assignment.rb
;;  Обрабатывает операторы присваивания, включая сложные присваивания (например, +=, -=).
;; (defn parse-binary-expression
;;   "Парсер бинарных выражений с учетом приоритета операторов"
;;   [state]
;;   (letfn 
;;     [(parse-binary-expr [left min-precedence]
;;        (let [token (current-token state)]
;;          (if (or (nil? token) 
;;                  (not (contains? operator-precedence (:value token))))
;;            left
;;            (let [op-precedence (get operator-precedence (:value token))]
;;              (if (< op-precedence min-precedence)
;;                left
;;                (let [after-op (advance state)
;;                      right-state (parse-unary-expression after-op)
;;                      right (:ast right-state)
;;                      next-state (parse-binary-expr 
;;                                   (->BinaryExpr (:value token) left right)
;;                                   (inc op-precedence))]
;;                  next-state))))))]
    
;;     (let [left-state (parse-unary-expression state)]
;;       (parse-binary-expr (:ast left-state) 0))))
(defn parse-binary-expression
  "Парсер бинарных выражений"
  [state]
  (let [left-state (parse-primary-expression state)
        op-token (current-token left-state)]
    (if (and op-token (contains? operator-precedence (:value op-token)))
      (let [after-op (advance left-state)
            right-state (parse-primary-expression after-op)]
        (assoc right-state 
               :ast 
               (->BinaryExpr (:value op-token) 
                            (:ast left-state) 
                            (:ast right-state))))
      left-state)))


;; (defn parse-bitwise-arithmetic   [state] (state)) 
 ;; @doc/Parsing-Flow/bitwise-arithmetic.rb
 ;;  Обрабатывает побитовые арифметические операции, включая побитовые И, ИЛИ, исключающее ИЛИ и их комбинации.
(defn parse-bitwise-expression
  "Парсер битовых выражений с поддержкой операторов &, |, ^."
  [state]
  (let [left-state (parse-primary-expression state)]
    (if-let [op-token (current-token left-state)]
      (cond
        (and-bitwise? op-token)
        (let [after-op (advance left-state)
              right-state (parse-primary-expression after-op)]
          (assoc right-state 
                 :ast 
                 (->BinaryExpr "&" 
                              (:ast left-state) 
                              (:ast right-state))))
        
        (or-bitwise? op-token)
        (let [after-op (advance left-state)
              right-state (parse-primary-expression after-op)]
          (assoc right-state 
                 :ast 
                 (->BinaryExpr "|" 
                              (:ast left-state) 
                              (:ast right-state))))
        
        (xor-bitwise? op-token)
        (let [after-op (advance left-state)
              right-state (parse-primary-expression after-op)]
          (assoc right-state 
                 :ast 
                 (->BinaryExpr "^" 
                              (:ast left-state) 
                              (:ast right-state))))
        
        :else left-state)
      left-state)))

(defn parse-block
  "Парсер блоков кода с использованием конечного автомата.
   Обрабатывает блоки кода, заключенные в фигурные скобки, 
   и накапливает операторы внутри блока."
  [state]
  (let [fsm-state (atom {:stage :start
                         :statements []
                         :depth 0})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (open-curly-bracket? token)
            (do 
              (reset! fsm-state 
                      {:stage :parsing-statements
                       :statements []
                       :depth 1})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected opening curly bracket")))
          
          :parsing-statements
          (cond
            (nil? token)
            (handle-error 
              (assoc current-state :error "Unexpected end of block"))
            
            (close-curly-bracket? token)
            (let [new-depth (dec (:depth @fsm-state))]
              (if (zero? new-depth)
                (-> current-state
                    advance
                    (#(assoc % :ast (->Block (:statements @fsm-state)))))
                (do 
                  (swap! fsm-state update :depth dec)
                  (recur (advance current-state)))))
            
            (open-curly-bracket? token)
            (do 
              (swap! fsm-state update :depth inc)
              (recur (advance current-state)))
            
            :else
            (let [statement-state (parse-statement current-state)]
              (if (:ast statement-state)
                (do 
                  (swap! fsm-state update :statements conj (:ast statement-state))
                  (recur statement-state))
                (recur (advance current-state)))))
          
          (handle-error 
            (assoc current-state :error "Invalid block parsing state")))))))

(defn parse-break
  "Парсер оператора break с обработкой ошибок через FSM.
   Поддерживает только простой оператор break;"
  [state]
  (let [fsm-state (atom {:stage :start})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (= (:value token) "break")
            (do 
              (reset! fsm-state {:stage :break-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'break' keyword")))
          
          :break-keyword
          (if (semicolon? token)
            (-> current-state
                advance
                (#(assoc % :ast {:type :break-statement})))
            
            (handle-error 
              (assoc current-state :error "Expected semicolon after break")))
          
          (handle-error 
            (assoc current-state :error "Invalid break statement parsing state")))))))

(defn parse-switch-case
  "Парсер оператора switch-case с расширенной семантикой.
   Поддерживает сложную структуру switch-case с множественными case и optional default."
  [state]
  (let [fsm-state (atom {:stage :start
                         :switch-expr nil
                         :cases []
                         :default nil})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (= (:value token) "switch")
            (do 
              (reset! fsm-state {:stage :switch-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'switch' keyword")))
          
          :switch-keyword
          (if (open-round-bracket? token)
            (do 
              (reset! fsm-state {:stage :switch-expr})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected opening round bracket")))
          
          :switch-expr
          (let [expr-state (parse-expression current-state)]
            (reset! fsm-state 
                    (update @fsm-state 
                            :switch-expr 
                            (constantly (:ast expr-state))))
            (recur expr-state))
          
          :switch-body
          (if (open-curly-bracket? token)
            (do 
              (reset! fsm-state {:stage :parse-cases})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected opening curly bracket")))
          
          :parse-cases
          (cond
            (= (:value token) "case")
            (let [case-state (parse-case current-state)]
              (reset! fsm-state 
                      (update @fsm-state 
                              :cases 
                              conj (:ast case-state)))
              (recur case-state))
            
            (= (:value token) "default")
            (let [default-state (parse-default current-state)]
              (reset! fsm-state 
                      (update @fsm-state 
                              :default 
                              (constantly (:ast default-state))))
              (recur default-state))
            
            (close-curly-bracket? token)
            (-> current-state
                advance
                (#(assoc % :ast 
                    {:type :switch-statement
                     :expr (:switch-expr @fsm-state)
                     :cases (:cases @fsm-state)
                     :default (:default @fsm-state)})))
            
            :else 
            (handle-error 
              (assoc current-state :error "Invalid switch-case parsing state")))
          
          (handle-error 
            (assoc current-state :error "Invalid switch statement parsing state")))))))
 ;; @doc/Parsing-Flow/switch-case.rb
 ;;  Обрабатывает оператор case, который используется для выбора одного из нескольких вариантов в зависимости от значения переменной.
(defn parse-condition   [state] (state)) 
 ;;  Обрабатывает условия для управляющих конструкций, таких как if, while, и for.
(defn parse-conditional-operator   [state] (state)) 
 ;;  Обрабатывает тернарный оператор (условный оператор) ? :, если он будет поддерживаться.
(defn parse-constant
  "Парсер константных значений.
   Обрабатывает целочисленные и символьные константы."
  [state]
  (let [token (current-token state)]
    (cond
      (int-number? token)
      (-> state
          advance
          (#(assoc % :ast (->Literal :int (:value token)))))
      
      (hex-number? token)
      (-> state
          advance
          (#(assoc % :ast (->Literal :hex (:value token)))))
      
      :else 
      (handle-error 
        (assoc state :error "Expected constant value")))))
 ;;  Обрабатывает константы, включая целочисленные и символьные константы.
(defn parse-continue
  "Парсер оператора continue с обработкой ошибок через FSM.
   Поддерживает только простой оператор continue;"
  [state]
  (let [fsm-state (atom {:stage :start})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (= (:value token) "continue")
            (do 
              (reset! fsm-state {:stage :continue-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'continue' keyword")))
          
          :continue-keyword
          (if (semicolon? token)
            (-> current-state
                advance
                (#(assoc % :ast {:type :continue-statement})))
            
            (handle-error 
              (assoc current-state :error "Expected semicolon after continue")))
          
          (handle-error 
            (assoc current-state :error "Invalid continue statement parsing state")))))))

(defn parse-do-while-loop   [state] (state)) 
 ;; @doc/Parsing-Flow/do-while-loop.rb
 ;;  Специфическая процедура для разбора циклов do-while.
(defn parse-for-loop
  "Парсер цикла for с расширенной семантикой и обработкой ошибок через FSM.
   Поддерживает различные варианты инициализации, условия и шага."
  [state]
  (let [fsm-state (atom {:stage :start
                         :init nil
                         :condition nil
                         :step nil
                         :body nil})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (type-for-keyword? token)
            (do 
              (reset! fsm-state {:stage :for-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'for' keyword")))
          
          :for-keyword
          (if (open-round-bracket? token)
            (do 
              (reset! fsm-state {:stage :init-section})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected opening round bracket")))
          
          :init-section
          (let [init-state (parse-statement current-state)]
            (reset! fsm-state 
                    (update @fsm-state 
                            :init 
                            (constantly (:ast init-state))))
            (recur init-state))
          
          :condition-section
          (let [condition-state (parse-condition current-state)]
            (reset! fsm-state 
                    (update @fsm-state 
                            :condition 
                            (constantly (:ast condition-state))))
            (recur condition-state))
          
          :step-section
          (let [step-state (parse-expression current-state)]
            (reset! fsm-state 
                    (update @fsm-state 
                            :step 
                            (constantly (:ast step-state))))
            (recur step-state))
          
          :body-section
          (let [body-state (parse-block current-state)]
            (-> body-state
                (#(assoc % :ast 
                    (->ForLoop 
                      (:init @fsm-state)
                      (:condition @fsm-state)
                      (:step @fsm-state)
                      (:ast body-state))))))
          
          (handle-error 
            (assoc current-state :error "Invalid for loop parsing state")))))))

(defn parse-function-call   [state] (state)) 
 ;; @doc/Parsing-Flow/function.rb
 ;;  Обрабатывает вызовы функций, включая передачу аргументов и обработку возвращаемых значений.
(defn parse-function-declaration
  "Парсер объявления функций с расширенной семантикой.
   Поддерживает:
   1. Базовое объявление: void foo(void) { ... }
   2. С прерыванием: void foo(void) interrupt 1 { ... }
   3. С указанием банка: void foo(void) using 2 { ... }
   4. Комбинированное: void foo(void) interrupt 1 using 2 { ... }"
  [state]
  (let [fsm-state (atom {:stage :start
                         :return-type nil
                         :name nil
                         :params []
                         :interrupt nil  ;; Номер прерывания (опционально)
                         :using nil})]   ;; Номер банка регистров (опционально)
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          ;; Начальное состояние - ожидаем тип возвращаемого значения
          :start
          (cond
            (type-void-keyword? token)
            (do 
              (reset! fsm-state 
                      {:stage :return-type
                       :return-type :void
                       :name nil
                       :params []
                       :interrupt nil
                       :using nil})
              (recur (advance current-state)))
            
            (type-int-keyword? token)
            (do 
              (reset! fsm-state 
                      {:stage :return-type
                       :return-type :int
                       :name nil
                       :params []
                       :interrupt nil
                       :using nil})
              (recur (advance current-state)))
            
            :else 
            (handle-error 
              (assoc current-state :error "Expected return type")))
          
          ;; После типа ожидаем имя функции
          :return-type
          (if (identifier? token)
            (do 
              (reset! fsm-state 
                      (assoc @fsm-state 
                             :name (:value token)
                             :stage :function-name))
              (recur (advance current-state)))
            (handle-error 
              (assoc current-state :error "Expected function name")))
          
          ;; После имени ожидаем открывающую скобку параметров
          :function-name
          (if (open-round-bracket? token)
            (do 
              (reset! fsm-state 
                      (assoc @fsm-state :stage :parse-params))
              (recur (advance current-state)))
            (handle-error 
              (assoc current-state :error "Expected opening round bracket")))
          
          ;; Парсим параметры функции
          :parse-params
          (let [params-state (parse-function-params current-state)]
            (if (:params params-state)
              (do 
                (reset! fsm-state 
                        (assoc @fsm-state 
                               :params (:params params-state)
                               :stage :check-specifiers))
                (recur params-state))
              (handle-error 
                (assoc current-state :error "Invalid function parameters"))))
          
          ;; Проверяем наличие спецификаторов interrupt и using
          :check-specifiers
          (cond
            ;; Если встретили interrupt
            (interrupt-keyword? token)
            (let [next-state (advance current-state)
                  int-token (current-token next-state)]
              (if (int-number? int-token)
                (do
                  (reset! fsm-state 
                          (assoc @fsm-state 
                                 :interrupt (:value int-token)
                                 :stage :check-using))
                  (recur (advance next-state)))
                (handle-error 
                  (assoc next-state :error "Expected interrupt number"))))
            
            ;; Если встретили using
            (using-keyword? token)
            (let [next-state (advance current-state)
                  int-token (current-token next-state)]
              (if (int-number? int-token)
                (do
                  (reset! fsm-state 
                          (assoc @fsm-state 
                                 :using (:value int-token)
                                 :stage :check-body))
                  (recur (advance next-state)))
                (handle-error 
                  (assoc next-state :error "Expected using number"))))
            
            ;; Если встретили открывающую фигурную скобку
            (open-curly-bracket? token)
            (do
              (reset! fsm-state 
                      (assoc @fsm-state :stage :parse-body))
              (recur current-state))
            
            :else 
            (handle-error 
              (assoc current-state :error "Expected interrupt, using, or function body")))
          
          ;; Проверяем using после interrupt
          :check-using
          (cond
            (using-keyword? token)
            (let [next-state (advance current-state)
                  int-token (current-token next-state)]
              (if (int-number? int-token)
                (do
                  (reset! fsm-state 
                          (assoc @fsm-state 
                                 :using (:value int-token)
                                 :stage :check-body))
                  (recur (advance next-state)))
                (handle-error 
                  (assoc next-state :error "Expected using number"))))
            
            (open-curly-bracket? token)
            (do
              (reset! fsm-state 
                      (assoc @fsm-state :stage :parse-body))
              (recur current-state))
            
            :else 
            (handle-error 
              (assoc current-state :error "Expected using or function body")))
          
          ;; Проверяем тело функции после всех спецификаторов
          :check-body
          (if (open-curly-bracket? token)
            (do
              (reset! fsm-state 
                      (assoc @fsm-state :stage :parse-body))
              (recur current-state))
            (handle-error 
              (assoc current-state :error "Expected function body")))
          
          ;; Парсим тело функции
          :parse-body
          (let [block-state (parse-block current-state)]
            (if (:ast block-state)
              (-> block-state
                  (#(assoc % :ast 
                      (->FunctionDecl 
                        (:return-type @fsm-state)
                        (:name @fsm-state)
                        (:params @fsm-state)
                        (:interrupt @fsm-state)
                        (:using @fsm-state)
                        (:ast block-state)))))
              (handle-error 
                (assoc current-state :error "Invalid function body"))))
          
          ;; Обработка неизвестных состояний
          (handle-error 
            (assoc current-state :error "Invalid function declaration parsing state")))))))

(defn parse-function-params   [state] (state)) 
 ;; @doc/Parsing-Flow/function-param.rb
 ;;  Обрабатывает список параметров функции, включая их типы и имена.

(defn parse-goto
  "Парсер оператора goto с обработкой ошибок через FSM.
   Поддерживает переход к метке в программе."
  [state]
  (let [fsm-state (atom {:stage :start
                         :label nil})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (= (:value token) "goto")
            (do 
              (reset! fsm-state {:stage :goto-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'goto' keyword")))
          
          :goto-keyword
          (if (identifier? token)
            (do 
              (reset! fsm-state 
                      (update @fsm-state 
                              :label 
                              (constantly (:value token))))
              (recur (advance current-state)))

            (handle-error 
              (assoc current-state :error "Expected label identifier")))
          
          :label
          (if (semicolon? token)
            (-> current-state
                advance
                (#(assoc % :ast 
                    {:type :goto-statement
                     :label (:label @fsm-state)})))
            
            (handle-error 
              (assoc current-state :error "Expected semicolon after label")))
          
          (handle-error 
            (assoc current-state :error "Invalid goto statement parsing state")))))))

  
 ;; @doc/Parsing-Flow/goto.rb
 ;;  Обрабатывает оператор goto, который используется для перехода к определенной метке в программе.
(defn parse-identifier
  "Парсер идентификаторов с расширенной семантикой.
   Поддерживает простые и составные идентификаторы."
  [state]
  (let [fsm-state (atom {:stage :start 
                         :result nil 
                         :current-token nil})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (identifier? token)
            (-> current-state
                advance
                (#(assoc % :ast (->Identifier (:value token)))))
            
            (handle-error 
              (assoc current-state :error "Expected identifier")))
          
          :identifier
          (cond 
            ;; Поддержка состыковки с точкой (составные идентификаторы)
            (dot? token)
            (do 
              (reset! fsm-state 
                      {:stage :dot 
                       :result (:result @fsm-state)
                       :current-token token})
              (recur (advance current-state)))
            
            ;; Поддержка массивов
            (open-square-bracket? token)
            (do 
              (reset! fsm-state 
                      {:stage :array-index 
                       :result (:result @fsm-state)
                       :current-token token})
              (recur (advance current-state)))
            
            :else 
            (-> current-state
                (#(assoc % :ast (:result @fsm-state)))))
          
          :dot
          (if (identifier? token)
            (do 
              (reset! fsm-state 
                      {:stage :identifier
                       :result (->Identifier 
                                 (str (:value (:result @fsm-state)) 
                                      "." 
                                      (:value token)))
                       :current-token token})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected identifier after dot")))
          
          :array-index
          (if (int-number? token)
            (do 
              (reset! fsm-state 
                      {:stage :close-array
                       :result (->ArrayType 
                                 (:result @fsm-state) 
                                 [(:value token)])
                       :current-token token})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected array index")))
          
          :close-array
          (if (close-square-bracket? token)
            (-> current-state
                (#(assoc % :ast (:result @fsm-state))))
            
            (handle-error 
              (assoc current-state :error "Expected closing square bracket"))))))))

(defn parse-increment-decrement   [state] (state)) 
 ;;  Обрабатывает инкременты и декременты.
(defn parse-interrupt-declaration   [state] (state)) 
 ;;  Обрабатывает объявления прерываний.
(defn parse-label   [state] (state)) 
 ;; @doc/Parsing-Flow/label.rb
 ;;  Обрабатывает метки, которые используются для обозначения определенных точек в программе.
(defn parse-literal
  "Парсер литералов: целые числа (десятичные и шестнадцатеричные)"
  [state]
  (let [token (current-token state)]
    (cond
      (int-number? token)
      (-> state
          advance
          (#(assoc % :ast (->Literal :int (:value token)))))
      
      (hex-number? token)
      (-> state
          advance
          (#(assoc % :ast (->Literal :hex (:value token)))))
      
      :else 
      (handle-error 
        (assoc state :error "Expected integer or hex literal")))))

(defn parse-logical-expressions   [state] (state)) 
 ;;  Обрабатывает логические выражения, включая операторы && и ||.
(defn parse-nested-block   [state] (state)) 
 ;;  Обрабатывает вложенные блоки кода, что может быть полезно для обработки сложных структур.
(defn parse-nested-statements   [state] (state)) 
 ;;  Обрабатывает вложенные операторы, включая операторы if, while, и for.
;; (defn parse-primary-expression
;;   "Парсер простых выражений: литералы, идентификаторы, скобки"
;;   [state]
;;   (let [token (current-token state)]
;;     (cond
;;       (or (int-number? token) (hex-number? token))
;;       (parse-literal state)
      
;;       (identifier? token)
;;       (parse-identifier state)
      
;;       (open-round-bracket? token)
;;       ;; Обработка выражения в скобках
;;       (let [after-open (advance state)
;;             expr-state (parse-expression after-open)
;;             close-token (current-token expr-state)]
;;         (if (close-round-bracket? close-token)
;;           (-> expr-state
;;               advance
;;               (#(assoc % :ast (:ast expr-state))))
;;           (handle-error 
;;             (assoc expr-state :error "Expected closing parenthesis"))))
      
;;       :else 
;;       (handle-error 
;;         (assoc state :error "Unexpected token in primary expression")))))

(defn parse-primary-expression
  "Парсер простых выражений: литералы и идентификаторы"
  [state]
  (let [token (current-token state)]
    (cond
      (or (int-number? token) (hex-number? token))
      (parse-literal state)
      
      (identifier? token)
      (parse-identifier state)
      
      :else 
      (handle-error 
        (assoc state :error "Unexpected token in primary expression")))))


;; -----------------begin-------------------------
(defn parse-program
  "Парсер программы с расширенной семантикой и обработкой ошибок через FSM.
   Поддерживает различные типы деклараций: функции, SFR, SBIT, переменные."
  [state]
  (let [fsm-state (atom {:stage :start
                         :declarations []
                         :current-declaration nil})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (cond
            (nil? token)
            (-> current-state
                (#(assoc % :ast 
                    (->Program (:declarations @fsm-state)))))
            
            (type-void-keyword? token)
            (do 
              (reset! fsm-state {:stage :function-declaration})
              (recur current-state))
            
            (type-int-keyword? token)
            (do 
              (reset! fsm-state {:stage :function-or-variable-declaration})
              (recur current-state))
            
            (sfr-keyword? token)
            (do 
              (reset! fsm-state {:stage :sfr-declaration})
              (recur current-state))
            
            (sbit-keyword? token)
            (do 
              (reset! fsm-state {:stage :sbit-declaration})
              (recur current-state))
            
            :else 
            (handle-error 
              (assoc current-state :error "Unexpected token in program parsing")))
          
          :function-declaration
          (let [decl-state (parse-function-declaration current-state)]
            (if (:ast decl-state)
              (do 
                (reset! fsm-state 
                        (update @fsm-state 
                                :declarations 
                                conj (:ast decl-state)))
                (recur decl-state))
              (handle-error 
                (assoc current-state :error "Invalid function declaration"))))
          
          :function-or-variable-declaration
          (let [type-state (parse-type current-state)]
            (if (:ast type-state)
              (let [identifier-state (parse-identifier type-state)]
                (cond
                  (open-round-bracket? (current-token identifier-state))
                  (let [params-state (parse-function-params identifier-state)]
                    (if (open-curly-bracket? (current-token params-state))
                      (let [block-state (parse-block params-state)]
                        (reset! fsm-state 
                                (update @fsm-state 
                                        :declarations 
                                        conj 
                                        (->FunctionDecl 
                                          (:ast type-state)
                                          (:ast identifier-state)
                                          (:params params-state)
                                          nil ;; interrupt
                                          nil ;; using
                                          (:ast block-state))))
                        (recur block-state))
                      (handle-error 
                        (assoc params-state :error "Expected opening curly bracket"))))
                  
                  (semicolon? (current-token identifier-state))
                  (do 
                    (reset! fsm-state 
                            (update @fsm-state 
                                    :declarations 
                                    conj 
                                    (->VarDecl 
                                      (:ast type-state)
                                      (:ast identifier-state))))
                    (recur (advance identifier-state)))
                  
                  :else 
                  (handle-error 
                    (assoc identifier-state :error "Unexpected token after identifier"))))
              (handle-error 
                (assoc type-state :error "Invalid type declaration"))))
          
          :sfr-declaration
          (let [sfr-state (parse-sfr-declaration current-state)]
            (if (:ast sfr-state)
              (do 
                (reset! fsm-state 
                        (update @fsm-state 
                                :declarations 
                                conj (:ast sfr-state)))
                (recur sfr-state))
              (handle-error 
                (assoc current-state :error "Invalid SFR declaration"))))
          
          :sbit-declaration
          (let [sbit-state (parse-sbit-declaration current-state)]
            (if (:ast sbit-state)
              (do 
                (reset! fsm-state 
                        (update @fsm-state 
                                :declarations 
                                conj (:ast sbit-state)))
                (recur sbit-state))
              (handle-error 
                (assoc current-state :error "Invalid SBIT declaration"))))
          
          (handle-error 
            (assoc current-state :error "Invalid program parsing state")))))))
;; -----------------end-------------------------











(defn parse-register   [state] (state)) 
 ;;  Обрабатывает регистры, включая регистры данных и регистры адреса.


(defn parse-return
  "Парсер оператора return с обработкой ошибок через FSM.
   Поддерживает:
   1. return; - возврат без значения 
   2. return expr; - возврат значения выражения"
  [state]
  (try
    (let [token (current-token state)]
      (if-not (type-return-keyword? token)
        (handle-error 
          (assoc state :error "Expected 'return' keyword"))
        
        (let [after-return (advance state)
              next-token (current-token after-return)]
          (if (semicolon? next-token)
            ;; return;
            (-> after-return 
                advance ;; пропускаем ;
                (#(assoc % :ast (->Return nil))))
            
            ;; return expr;
            (handle-error 
              (assoc state :error "Complex return statements not yet supported"))))))
    
    (catch Exception e
      (handle-error (assoc state :error (.getMessage e))))))


(defn parse-sbit-declaration
  "Парсер объявления SBIT с обработкой ошибок через FSM.
   Поддерживает объявление битовых регистров."
  [state]
  (let [fsm-state (atom {:stage :start
                         :name nil
                         :bit nil})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (sbit-keyword? token)
            (do 
              (reset! fsm-state {:stage :sbit-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'sbit' keyword")))
          
          :sbit-keyword
          (if (identifier? token)
            (do 
              (reset! fsm-state 
                      (update @fsm-state 
                              :name 
                              (constantly (:value token))))
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected SBIT name identifier")))
          
          :name
          (if (int-number? token)
            (do 
              (reset! fsm-state 
                      (update @fsm-state 
                              :bit 
                              (constantly (:value token))))
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected bit number")))
          
          :bit
          (if (semicolon? token)
            (-> current-state
                advance
                (#(assoc % :ast 
                    (->SbitDecl 
                      (:name @fsm-state)
                      (:bit @fsm-state)))))
            
            (handle-error 
              (assoc current-state :error "Expected semicolon after SBIT declaration")))
          
          (handle-error 
            (assoc current-state :error "Invalid SBIT declaration parsing state")))))))

(defn parse-sfr-declaration
  "Парсер объявления SFR с обработкой ошибок через FSM.
   Поддерживает объявление специальных функциональных регистров."
  [state]
  (let [fsm-state (atom {:stage :start
                         :name nil
                         :address nil})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (sfr-keyword? token)
            (do 
              (reset! fsm-state {:stage :sfr-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'sfr' keyword")))
          
          :sfr-keyword
          (if (identifier? token)
            (do 
              (reset! fsm-state 
                      (update @fsm-state 
                              :name 
                              (constantly (:value token))))
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected SFR name identifier")))
          
          :name
          (if (or (int-number? token) (hex-number? token))
            (do 
              (reset! fsm-state 
                      (update @fsm-state 
                              :address 
                              (constantly (:value token))))
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected address number")))
          
          :address
          (if (semicolon? token)
            (-> current-state
                advance
                (#(assoc % :ast 
                    (->SfrDecl 
                      (:name @fsm-state)
                      (:address @fsm-state)))))
            
            (handle-error 
              (assoc current-state :error "Expected semicolon after SFR declaration")))
          
          (handle-error 
            (assoc current-state :error "Invalid SFR declaration parsing state")))))))

(defn parse-statement
  "Универсальная функция для разбора различных операторов.
   Поддерживает объявления функций, переменных, 
   операторы возврата и специфичные для C51 конструкции."
  [state]
  (let [token (current-token state)]
    (cond
      (type-return-keyword? token)
      (parse-return state)
      
      (type-int-keyword? token)
      (let [type-state (parse-type state)]
        (if (:ast type-state)
          (let [identifier-state (parse-identifier type-state)]
            (if (semicolon? (current-token identifier-state))
              (-> identifier-state
                  advance
                  (#(assoc % :ast 
                      (->VarDecl (:ast type-state) 
                                 (:ast identifier-state)))))
              (handle-error 
                (assoc identifier-state :error "Expected semicolon"))))
          (handle-error 
            (assoc type-state :error "Invalid variable declaration"))))
      
      (type-void-keyword? token)
      (let [type-state (parse-type state)]
        (if (:ast type-state)
          (let [identifier-state (parse-identifier type-state)]
            (if (open-round-bracket? (current-token identifier-state))
              (let [after-open (advance identifier-state)
                    after-close (if (close-round-bracket? (current-token after-open))
                                 (advance after-open)
                                 (handle-error (assoc after-open :error "Expected closing parenthesis")))]
                (if (open-curly-bracket? (current-token after-close))
                  (let [block-state (parse-block after-close)]
                    (assoc block-state 
                           :ast 
                           (->FunctionDecl 
                             :void
                             (:ast identifier-state)
                             []
                             nil ;; interrupt
                             nil ;; using
                             (:ast block-state))))
                  (handle-error 
                    (assoc after-close :error "Expected opening curly bracket"))))
              (handle-error 
                (assoc identifier-state :error "Expected opening parenthesis"))))
          (handle-error 
            (assoc type-state :error "Invalid function declaration"))))
      
      :else 
      (handle-error 
        (assoc state :error "Unexpected statement")))))

(defn parse-type
  "Парсер типов данных с поддержкой модификаторов и сложных типов"
  [state]
  (let [fsm-state (atom {:stage :start 
                         :modifiers []
                         :base-type nil})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (cond
            (type-unsigned-keyword? token)
            (do 
              (reset! fsm-state 
                      {:stage :start 
                       :modifiers (conj (:modifiers @fsm-state) :unsigned)
                       :base-type nil})
              (recur (advance current-state)))
            
            (type-signed-keyword? token)
            (do 
              (reset! fsm-state 
                      {:stage :start 
                       :modifiers (conj (:modifiers @fsm-state) :signed)
                       :base-type nil})
              (recur (advance current-state)))
            
            (type-int-keyword? token)
            (do 
              (reset! fsm-state 
                      {:stage :base-type 
                       :modifiers (:modifiers @fsm-state)
                       :base-type :int})
              (recur (advance current-state)))
            
            (type-char-keyword? token)
            (do 
              (reset! fsm-state 
                      {:stage :base-type 
                       :modifiers (:modifiers @fsm-state)
                       :base-type :char})
              (recur (advance current-state)))
            
            (type-void-keyword? token)
            (do 
              (reset! fsm-state 
                      {:stage :base-type 
                       :modifiers (:modifiers @fsm-state)
                       :base-type :void})
              (recur (advance current-state)))
            
            :else 
            (handle-error 
              (assoc current-state :error "Unexpected type")))
          
          :base-type
          (cond
            ;; Указатель
            (multiply? token)
            (do 
              (reset! fsm-state 
                      {:stage :pointer-type
                       :modifiers (:modifiers @fsm-state)
                       :base-type (:base-type @fsm-state)})
              (recur (advance current-state)))
            
            ;; Массив
            (open-square-bracket? token)
            (do 
              (reset! fsm-state 
                      {:stage :array-dimensions
                       :modifiers (:modifiers @fsm-state)
                       :base-type (:base-type @fsm-state)
                       :dimensions []})
              (recur (advance current-state)))
            
            :else 
            (-> current-state
                (#(assoc % :ast 
                    (cond 
                      (contains? #{:pointer-type :array-type} (:base-type @fsm-state))
                      (->PointerType (:base-type @fsm-state))
                      
                      :else 
                      {:type (:base-type @fsm-state)
                       :modifiers (:modifiers @fsm-state)})))))
          
          :pointer-type
          (-> current-state
              (#(assoc % :ast 
                  (->PointerType (:base-type @fsm-state)))))
          
          :array-dimensions
          (if (int-number? token)
            (do 
              (reset! fsm-state 
                      {:stage :array-dimensions
                       :modifiers (:modifiers @fsm-state)
                       :base-type (:base-type @fsm-state)
                       :dimensions (conj (:dimensions @fsm-state) 
                                         (:value token))})
              (recur (advance current-state)))
            
            (if (close-square-bracket? token)
              (-> current-state
                  (#(assoc % :ast 
                      (->ArrayType (:base-type @fsm-state) 
                                   (:dimensions @fsm-state)))))
              
              (handle-error 
                (assoc current-state :error "Invalid array dimensions")))))))))

(defn parse-unary-expression
  "Парсер унарных выражений.
   Поддерживает:
   1. Префиксные операторы: ++x, --x, +x, -x, !x, ~x
   2. Постфиксные операторы: x++, x--"
  [state]
  (let [token (current-token state)]
    (cond
      ;; Префиксные операторы
      (increment? token)
      (let [after-op (advance state)
            operand-state (parse-identifier after-op)]
        (assoc operand-state 
               :ast 
               (->UnaryExpr :pre-increment "++" (:ast operand-state))))
      
      (decrement? token)
      (let [after-op (advance state)
            operand-state (parse-identifier after-op)]
        (assoc operand-state 
               :ast 
               (->UnaryExpr :pre-decrement "--" (:ast operand-state))))
      
      (plus? token)
      (let [after-op (advance state)
            operand-state (parse-primary-expression after-op)]
        (assoc operand-state 
               :ast 
               (->UnaryExpr :unary-plus "+" (:ast operand-state))))
      
      (minus? token)
      (let [after-op (advance state)
            operand-state (parse-primary-expression after-op)]
        (assoc operand-state 
               :ast 
               (->UnaryExpr :negation "-" (:ast operand-state))))
      
      (not-logical? token)
      (let [after-op (advance state)
            operand-state (parse-primary-expression after-op)]
        (assoc operand-state 
               :ast 
               (->UnaryExpr :logical-not "!" (:ast operand-state))))
      
      (not-bitwise? token)
      (let [after-op (advance state)
            operand-state (parse-primary-expression after-op)]
        (assoc operand-state 
               :ast 
               (->UnaryExpr :bitwise-not "~" (:ast operand-state))))
      
      ;; Постфиксные операторы
      (identifier? token)
      (let [id-state (parse-identifier state)
            next-token (current-token id-state)]
        (cond
          (increment? next-token)
          (-> id-state
              advance
              (#(assoc % :ast 
                  (->UnaryExpr :post-increment "++" (:ast id-state)))))
          
          (decrement? next-token)
          (-> id-state
              advance
              (#(assoc % :ast 
                  (->UnaryExpr :post-decrement "--" (:ast id-state)))))
          
          :else id-state))
      
      ;; Если нет унарного оператора, парсим простое выражение
      :else 
      (parse-primary-expression state))))

(defn parse-using-declaration   [state] (state)) 
 ;;  Обрабатывает объявления using, которые используются для указания переменных, которые будут использоваться в программе.
(defn parse-while-loop   [state] (state)) 
 ;; @doc/Parsing-Flow/while-loop.rb
 ;;  Специфическая процедура для разбора циклов while.

(defn parse-case
  "Парсер оператора case с расширенной семантикой и обработкой ошибок через FSM.
   Поддерживает разбор case с константными выражениями и блоками кода."
  [state]
  (let [fsm-state (atom {:stage :start
                         :value nil
                         :statements []})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (= (:value token) "case")
            (do 
              (reset! fsm-state {:stage :case-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'case' keyword")))
          
          :case-keyword
          (let [const-state (parse-constant current-state)]
            (if (:ast const-state)
              (do 
                (reset! fsm-state 
                        (update @fsm-state 
                                :value 
                                (constantly (:ast const-state))))
                (recur const-state))
              (handle-error 
                (assoc current-state :error "Expected constant value after case"))))
          
          :value
          (if (colon? token)
            (do 
              (reset! fsm-state {:stage :parse-statements})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected colon after case value")))
          
          :parse-statements
          (cond
            (or (= (:value token) "case") 
                (= (:value token) "default") 
                (close-curly-bracket? token))
            (-> current-state
                (#(assoc % :ast 
                    {:type :case-statement
                     :value (:value @fsm-state)
                     :statements (:statements @fsm-state)})))
            
            :else
            (let [statement-state (parse-statement current-state)]
              (if (:ast statement-state)
                (do 
                  (reset! fsm-state 
                          (update @fsm-state 
                                  :statements 
                                  conj (:ast statement-state)))
                  (recur statement-state))
                (recur (advance current-state)))))
          
          (handle-error 
            (assoc current-state :error "Invalid case parsing state")))))))

(defn parse-default
  "Парсер оператора default с расширенной семантикой и обработкой ошибок через FSM.
   Поддерживает разбор default блока с множественными операторами."
  [state]
  (let [fsm-state (atom {:stage :start
                         :statements []})]
    (loop [current-state state]
      (let [token (current-token current-state)]
        (case (:stage @fsm-state)
          :start
          (if (= (:value token) "default")
            (do 
              (reset! fsm-state {:stage :default-keyword})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected 'default' keyword")))
          
          :default-keyword
          (if (colon? token)
            (do 
              (reset! fsm-state {:stage :parse-statements})
              (recur (advance current-state)))
            
            (handle-error 
              (assoc current-state :error "Expected colon after default")))
          
          :parse-statements
          (cond
            (or (= (:value token) "case") 
                (= (:value token) "default") 
                (close-curly-bracket? token))
            (-> current-state
                (#(assoc % :ast 
                    {:type :default-statement
                     :statements (:statements @fsm-state)})))
            
            :else
            (let [statement-state (parse-statement current-state)]
              (if (:ast statement-state)
                (do 
                  (reset! fsm-state 
                          (update @fsm-state 
                                  :statements 
                                  conj (:ast statement-state)))
                  (recur statement-state))
                (recur (advance current-state)))))
          
          (handle-error 
            (assoc current-state :error "Invalid default parsing state")))))))
