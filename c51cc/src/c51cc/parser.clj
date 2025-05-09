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
          advance                     ;;	 Перемещается к следующему токену
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
          parse-condition 	          ;;	 Обрабатывает условия для управляющих конструкций, таких как if, while, и for.
          parse-conditional-operator 	;;	 Обрабатывает тернарный оператор (условный оператор) ? :, если он будет поддерживаться.
          parse-const 	              ;;	 Обрабатывает константы, включая целочисленные и символьные константы.
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
          parse-switch              	;;	 Обрабатывает оператор switch, который используется для выбора одного из нескольких вариантов в зависимости от значения переменной.
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
  (when (< (:position state) (count (:tokens state)))
    (nth (:tokens state) (:position state))))

(defn peek-next-token [state]
  (when (< (inc (:position state)) (count (:tokens state)))
    (nth (:tokens state) (inc (:position state)))))

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

(defn handle-error   [state] (state)) 
 ;; @doc/Parsing-Flow/handle-error.rb
 ;;  Обработка ошибок синтаксического анализа, которая может включать вывод сообщений об ошибках и восстановление состояния парсера.
(defn parse-array-access   [state] (state)) 
 ;;  Обрабатывает доступ к элементам массивов, включая индексацию.
(defn parse-array-dimensions   [state] (state)) 
 ;;  Обрабатывает размерности массивов, если они присутствуют в объявлении.
(defn parse-array-type   [state] (state)) 
 ;;  Обрабатывает объявления массивов, включая их размерности.
(defn parse-assignment   [state] (state))
;; @doc/Parsing-Flow/assignment.rb
 ;;  Обрабатывает операторы присваивания, включая сложные присваивания (например, +=, -=).
(defn parse-binary-expression   [state] (state)) 
;; @doc/Parsing-Flow/binary-expression.rb
 ;;  Обрабатывает бинарные выражения с учетом приоритета операторов.
(defn parse-bitwise-arithmetic   [state] (state)) 
 ;; @doc/Parsing-Flow/bitwise-arithmetic.rb
 ;;  Обрабатывает побитовые арифметические операции, включая побитовые И, ИЛИ, исключающее ИЛИ и их комбинации.
(defn parse-bitwise-expression   [state] (state)) 
 ;; @doc/Parsing-Flow/bitwise-expressions.rb
 ;;  Обрабатывает битовые операции, включая побитовые И, ИЛИ, исключающее ИЛИ и их комбинации.
(defn parse-block   [state] (state)) 
 ;; @doc/Parsing-Flow/block.rb
 ;;  Обрабатывает блоки кода, заключенные в фигурные скобки, и накапливает операторы внутри блока.
(defn parse-break   [state] (state)) 
 ;; @doc/Parsing-Flow/break.rb
 ;;  Обрабатывает оператор break, который используется для выхода из циклов.
(defn parse-case   [state] (state)) 
 ;; @doc/Parsing-Flow/switch-case.rb
 ;;  Обрабатывает оператор case, который используется для выбора одного из нескольких вариантов в зависимости от значения переменной.
(defn parse-condition   [state] (state)) 
 ;;  Обрабатывает условия для управляющих конструкций, таких как if, while, и for.
(defn parse-conditional-operator   [state] (state)) 
 ;;  Обрабатывает тернарный оператор (условный оператор) ? :, если он будет поддерживаться.
(defn parse-constant   [state] (state)) 
 ;;  Обрабатывает константы, включая целочисленные и символьные константы.
(defn parse-continue   [state] (state)) 
 ;; @doc/Parsing-Flow/continue.rb
 ;;  Обрабатывает оператор continue, который используется для перехода к следующей итерации цикла.
(defn parse-do-while-loop   [state] (state)) 
 ;; @doc/Parsing-Flow/do-while-loop.rb
 ;;  Специфическая процедура для разбора циклов do-while.
(defn parse-for-loop   [state] (state)) 
 ;; @doc/Parsing-Flow/for-loop.rb
 ;;  Специфическая процедура для разбора циклов for, включая инициализацию, условие и шаг.
(defn parse-function-call   [state] (state)) 
 ;; @doc/Parsing-Flow/function.rb
 ;;  Обрабатывает вызовы функций, включая передачу аргументов и обработку возвращаемых значений.
(defn parse-function-declaration   [state] (state)) 
 ;;  Обрабатывает объявления функций, включая параметры, возвращаемый тип и тело функции.
(defn parse-function-params   [state] (state)) 
 ;; @doc/Parsing-Flow/function-param.rb
 ;;  Обрабатывает список параметров функции, включая их типы и имена.
(defn parse-goto   [state] (state)) 
 ;; @doc/Parsing-Flow/goto.rb
 ;;  Обрабатывает оператор goto, который используется для перехода к определенной метке в программе.
(defn parse-identifier   [state] (state)) 
 ;;  Обрабатывает идентификаторы, проверяя их корректность и возвращая соответствующие узлы.
(defn parse-increment-decrement   [state] (state)) 
 ;;  Обрабатывает инкременты и декременты.
(defn parse-interrupt-declaration   [state] (state)) 
 ;;  Обрабатывает объявления прерываний.
(defn parse-label   [state] (state)) 
 ;; @doc/Parsing-Flow/label.rb
 ;;  Обрабатывает метки, которые используются для обозначения определенных точек в программе.
(defn parse-literal   [state] (state)) 
 ;;  Обрабатывает различные типы литералов, такие как числовые, символьные и булевы значения.
(defn parse-logical-expressions   [state] (state)) 
 ;;  Обрабатывает логические выражения, включая операторы && и ||.
(defn parse-nested-block   [state] (state)) 
 ;;  Обрабатывает вложенные блоки кода, что может быть полезно для обработки сложных структур.
(defn parse-nested-statements   [state] (state)) 
 ;;  Обрабатывает вложенные операторы, включая операторы if, while, и for.
(defn parse-primary-expression   [state] (state)) 
 ;;  Обрабатывает простые выражения, такие как литералы, идентификаторы и вложенные выражения в скобках.
(defn parse-program   [state] (state)) 
 ;;  Основной парсер программы, который принимает список токенов и возвращает структуру программы, содержащую все декларации.
(defn parse-register   [state] (state)) 
 ;;  Обрабатывает регистры, включая регистры данных и регистры адреса.
(defn parse-return   [state] (state)) 
 ;;  Обрабатывает оператор return, который используется для возврата значения из функции.
(defn parse-sbit-declaration   [state] (state)) 
 ;; @doc/Parsing-Flow/sbit-declaration.rb
 ;;  Обрабатывает объявления битовых регистров.
(defn parse-sfr-declaration   [state] (state)) 
 ;; @doc/Parsing-Flow/sfr-declaration.rb
 ;;  Обрабатывает объявления SFR.
(defn parse-statement   [state] (state)) 
 ;; @doc/Parsing-Flow/statement.rb
 ;;  Универсальная функция для разбора различных операторов, включая объявления функций, переменных, операторов возврата и специфичных для C51 конструкций.
(defn parse-switch   [state] (state)) 
 ;; @doc/Parsing-Flow/switch-case.rb
 ;;  Обрабатывает оператор switch, который используется для выбора одного из нескольких вариантов в зависимости от значения переменной.
(defn parse-type   [state] (state)) 
 ;;  Обрабатывает различные типы данных, включая базовые типы и массивы.
(defn parse-unary-expression   [state] (state)) 
 ;; @doc/Parsing-Flow/unary-expression.rb
 ;;  Обрабатывает унарные выражения, такие как инкременты и декременты.
(defn parse-using-declaration   [state] (state)) 
 ;;  Обрабатывает объявления using, которые используются для указания переменных, которые будут использоваться в программе.
(defn parse-while-loop   [state] (state)) 
 ;; @doc/Parsing-Flow/while-loop.rb
 ;;  Специфическая процедура для разбора циклов while.
