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
(def parse-expression 
  "Основной парсер выражений. 
   Абстракция над binary-expression для гибкости и расширяемости."
  parse-binary-expression)


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
  (let [tokens (:tokens state)
        position (:position state)]
    (when (and (< position (count tokens))
               (not-empty tokens))
      (nth tokens position))))

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
      state)))

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
  [state]
  (let [fsm-state (atom :initial)
        sync-tokens #{:semicolon-separator :close-curly-bracket :close-round-bracket}
        token (current-token state)]
    
    ;; Логируем информацию об ошибке
    (log/error "Ошибка парсера:"
               "\n  - Текущий токен:" (when token 
                                       {:type (:type token) 
                                        :value (:value token)})
               "\n  - Позиция:" (:position state))
    
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
                current-state)
              
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
            current-state))))))

(defn parser [state]
  (parse-program state))


(defn parse-main-function [state] (state))

(defn parse-program
  "Основной парсер программы.
   Принимает вектор токенов и возвращает AST программы."
  [tokens]
  (log/debug "Начало парсинга программы. Количество токенов:" (count tokens))
  (let [initial-state (->ParserState tokens 0)
        fsm-state (atom {:stage :start 
                        :declarations []})]
    (loop [current-state initial-state]
      (let [token (current-token current-state)]
        (log/trace "Обработка токена:" 
                  (when token 
                    (str (:type token) " " (:value token)))
                  "Позиция:" (:position current-state))
        (if (nil? token)
          (do
            (log/debug "Парсинг завершен. Найдено деклараций:" 
                      (count (:declarations @fsm-state)))
            (->Program (:declarations @fsm-state)))
          
          ;; Иначе парсим очередную декларацию
          (case (:stage @fsm-state)
            :start
            (cond
              ;; Функция
              (or (type-void-keyword? token)
                  (type-int-keyword? token))
              (let [decl-state (parse-function-declaration current-state)]
                (if (:ast decl-state)
                  (do 
                    (swap! fsm-state update :declarations conj (:ast decl-state))
                    (recur decl-state))
                  (handle-error current-state)))
              
              ;; SFR
              (sfr-keyword? token)
              (let [decl-state (parse-sfr-declaration current-state)]
                (if (:ast decl-state)
                  (do 
                    (swap! fsm-state update :declarations conj (:ast decl-state))
                    (recur decl-state))
                  (handle-error current-state)))
              
              ;; SBIT
              (sbit-keyword? token)
              (let [decl-state (parse-sbit-declaration current-state)]
                (if (:ast decl-state)
                  (do 
                    (swap! fsm-state update :declarations conj (:ast decl-state))
                    (recur decl-state))
                  (handle-error current-state)))
              
              :else 
              (handle-error current-state))))))))

(defn parse-function-params
  "Парсер параметров функции.
   Обрабатывает:
   - Тип параметра (void/int/char)
   - Имя параметра (если есть)
   - Закрывающую скобку
   
   Возвращает вектор параметров и новое состояние"
  [state]
  (let [param-type-token (current-token state)]
    (if (type-void-keyword? param-type-token)
      ;; Если void - параметров нет
      [(step-next state) []]
      ;; TODO: Здесь будет обработка других параметров
      [(step-next state) []])))

(defn parse-function-declaration
  "Парсер объявления функции.
   Обрабатывает:
   - тип возвращаемого значения (void/int)
   - имя функции
   - параметры в скобках
   - открывающую скобку тела"
  [state]
  (let [return-type-token (current-token state)
        _ (log/debug "Парсинг функции. Тип возвращаемого значения:" (:type return-type-token))
        
        ;; Шаг 1: Получаем тип возвращаемого значения
        return-type (cond 
                     (type-void-keyword? return-type-token) :void
                     (type-int-keyword? return-type-token) :int
                     :else nil)
        
        ;; Шаг 2: Переходим к имени функции
        name-state (step-next state)
        name-token (current-token name-state)
        _ (log/debug "Имя функции:" (when name-token (:value name-token)))
        
        ;; Шаг 3: Проверяем открывающую скобку параметров
        params-state (step-next name-state)
        params-token (current-token params-state)]
    
    (if (and return-type 
             name-token
             (or (type-main-keyword? name-token)  ;; Разрешаем main
                 (identifier? name-token))
             (open-round-bracket? params-token))
      (let [;; Шаг 4: Парсим параметры
            param-start-state (step-next params-state)
            [after-params-state params] (parse-function-params param-start-state)
            
            ;; Шаг 5: Проверяем закрывающую скобку
            close-paren-token (current-token after-params-state)
            _ (log/debug "Закрывающая скобка параметров:" 
                        (when close-paren-token (:type close-paren-token)))
            
            ;; Шаг 6: Проверяем открывающую фигурную скобку
            body-state (when (close-round-bracket? close-paren-token)
                        (step-next after-params-state))
            body-token (when body-state (current-token body-state))
            _ (log/debug "Открывающая скобка тела:" 
                        (when body-token (:type body-token)))]
        
        (if (and (close-round-bracket? close-paren-token)
                 (open-curly-bracket? body-token))
          ;; Все токены на месте - создаем AST
          (-> body-state
              step-next
              (assoc :ast 
                     (->FunctionDeclaration 
                       return-type
                       (:value name-token)
                       params        ;; Теперь передаем распарсенные параметры
                       nil           ;; interrupt
                       nil           ;; using
                       nil)))       ;; body
          ;; Ошибка в параметрах или скобках
          (do
            (log/error "Ошибка в объявлении функции (скобки):"
                      "\n  - Закрывающая скобка параметров:" 
                      (when close-paren-token (:type close-paren-token))
                      "\n  - Открывающая скобка тела:" 
                      (when body-token (:type body-token)))
            (step-next state))))
            
      ;; Ошибка в типе или имени функции
      (do
        (log/error "Ошибка в объявлении функции:"
                  "\n  - Тип возвращаемого значения:" (when return-type-token (:type return-type-token))
                  "\n  - Имя функции:" (when name-token (:value name-token))
                  "\n  - Открывающая скобка параметров:" (when params-token (:type params-token)))
        (step-next state)))))

(defn parse-sfr-declaration
  "Заглушка для парсера SFR.
   Просто возвращает тестовый AST для проверки работы основного парсера"
  [state]
  (let [token (current-token state)]
    (log/debug "Парсинг SFR, текущий токен:" (:type token) (:value token))
    (-> state
        step-next  ;; Продвигаемся к следующему токену
        (assoc :ast 
               (->SfrDeclaration 
                 "TEST_SFR"    ;; name
                 0x80)))))     ;; address

(defn parse-sbit-declaration
  "Заглушка для парсера SBIT.
   Просто возвращает тестовый AST для проверки работы основного парсера"
  [state]
  (let [token (current-token state)]
    (log/debug "Парсинг SBIT, текущий токен:" (:type token) (:value token))
    (-> state
        step-next  ;; Продвигаемся к следующему токену
        (assoc :ast 
               (->SbitDeclaration 
                 "TEST_SBIT"   ;; name
                 0)))))        ;; bit
