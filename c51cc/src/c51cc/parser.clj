(ns c51cc.parser
  (:require [c51cc.lexer :as lexer]
            [clojure.string :as str]))

;; Определяем структуру AST узлов
(defrecord Program [declarations])
(defrecord FunctionDecl [return-type name params body])
(defrecord VarDecl [type name value])
(defrecord Block [statements])
(defrecord ReturnStmt [expr])
(defrecord BinaryExpr [operator left right])
(defrecord Literal [type value])
(defrecord Identifier [name])
(defrecord InterruptDecl [number function])
(defrecord SfrDecl [name address])
(defrecord SbitDecl [name sfr bit])
(defrecord PointerType [base-type])
(defrecord ArrayType [base-type dimensions])
(defrecord StructType [name fields])
(defrecord StructField [type name])

;; Состояние парсера
(defrecord ParserState [tokens position])

;; Предварительные объявления всех функций
(declare parse-expression 
         parse-primary-expression 
         parse-statement 
         parse-block
         parse-function-declaration
         parse-pointer-type
         parse-struct-type
         parse-array-type
         expect-token)

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
  (let [token (current-token state)]
    (when (and (= (:type token) expected-type)
               (= (:value token) expected-value))
      (advance state))))

;; Расширяем parse-expression для поддержки бинарных операций
(def operator-precedence
  {"||" 1, "&&" 2, "|" 3, "^" 4, "&" 5,
   "==" 6, "!=" 6, "<" 7, ">" 7, "<=" 7, ">=" 7,
   "+" 8, "-" 8, "*" 9, "/" 9, "%" 9})

(defn binary-operator? [token]
  (contains? operator-precedence (:value token)))

(defn parse-binary-expression 
  ([state] (parse-binary-expression state 0))
  ([state min-precedence]
   (let [[state1 left] (parse-primary-expression state)]
     (loop [current-state state1
            current-left left]
       (let [op-token (current-token current-state)]
         (if (and op-token 
                  (binary-operator? op-token)
                  (>= (get operator-precedence (:value op-token)) min-precedence))
           (let [op-precedence (get operator-precedence (:value op-token))
                 next-state (advance current-state)
                 [new-state right] (parse-binary-expression next-state (inc op-precedence))]
             (recur new-state
                    (->BinaryExpr (:value op-token) current-left right)))
           [current-state current-left]))))))

(defn parse-primary-expression [state]
  (let [token (current-token state)]
    (cond
      (= (:type token) :number)
      [(advance state) (->Literal :number (:value token))]
      
      (identifier? token)
      [(advance state) (->Identifier (:value token))]
      
      (= (:type token) :bracket)
      (if (= (:value token) "(")
        (let [state1 (advance state)
              [state2 expr] (parse-expression state1)
              state3 (expect-token state2 :bracket ")")]
          [state3 expr])
        [state nil])
      
      :else
      [state nil])))

;; Заменяем старую версию parse-expression
(def parse-expression parse-binary-expression)

;; Основные функции парсинга
(defn parse-type [state]
  (let [token (current-token state)]
    (cond
      ;; Указатели
      (= (:value token) "*")
      (parse-pointer-type state)
      
      ;; Структуры
      (= (:value token) "struct")
      (parse-struct-type state)
      
      ;; Базовые типы
      (type-keyword? token)
      (let [base-state (advance state)
            next-token (current-token base-state)]
        (if (= (:value next-token) "[")
          (parse-array-type state)
          [(advance state) (:value token)]))
      
      :else
      [state nil])))

(defn parse-identifier [state]
  (let [token (current-token state)]
    (when (identifier? token)
      [(advance state) (->Identifier (:value token))])))

;; C51-специфичные парсеры
(defn parse-interrupt-declaration [state]
  (let [state1 (expect-token state :c51-keyword "interrupt")
        [state2 number] (parse-expression state1)
        [state3 func] (parse-function-declaration state2)]
    [state3 (->InterruptDecl number func)]))

(defn parse-sfr-declaration [state]
  (let [state1 (expect-token state :c51-keyword "sfr")
        [state2 name-node] (parse-identifier state1)
        state3 (expect-token state2 :assignment-operator "=")
        [state4 addr] (parse-expression state3)
        state5 (expect-token state4 :separator ";")]
    [state5 (->SfrDecl (:name name-node) addr)]))

(defn parse-sbit-declaration [state]
  (let [state1 (expect-token state :c51-keyword "sbit")
        [state2 name-node] (parse-identifier state1)
        state3 (expect-token state2 :assignment-operator "=")
        [state4 sfr-name] (parse-identifier state3)
        state5 (expect-token state4 :separator "^")
        [state6 bit-num] (parse-expression state5)
        state7 (expect-token state6 :separator ";")]
    [state7 (->SbitDecl (:name name-node) (:name sfr-name) bit-num)]))

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
        [state6 body] (parse-block state5)]
    [state6 (->FunctionDecl return-type (:name name-node) params body)]))

(defn parse-block [state]
  (let [state1 (expect-token state :bracket "{")
        [state2 statements] (loop [current-state state1
                                 stmts []]
                            (let [token (current-token current-state)]
                              (if (= (:value token) "}")
                                [(advance current-state) stmts]
                                (let [[new-state stmt] (parse-statement current-state)]
                                  (recur new-state (conj stmts stmt))))))]
    [state2 (->Block statements)]))

(defn parse-return [state]
  (let [state1 (advance state) ; пропускаем 'return'
        [state2 expr] (parse-expression state1)
        state3 (expect-token state2 :separator ";")]
    [state3 (->ReturnStmt expr)]))

(defn parse-statement [state]
  (let [token (current-token state)]
    (cond
      ;; C51-специфичные конструкции
      (and (c51-keyword? token) (= (:value token) "interrupt"))
      (parse-interrupt-declaration state)
      
      (and (c51-keyword? token) (= (:value token) "sfr"))
      (parse-sfr-declaration state)
      
      (and (c51-keyword? token) (= (:value token) "sbit"))
      (parse-sbit-declaration state)
      
      ;; Существующие конструкции
      (= (:value token) "return")
      (parse-return state)
      
      (type-keyword? token)
      (parse-function-declaration state)
      
      :else
      (let [[new-state expr] (parse-expression state)
            final-state (expect-token new-state :separator ";")]
        [final-state expr]))))

(defn parse-program [tokens]
  (loop [state (->ParserState tokens 0)
         declarations []]
    (if (>= (:position state) (count tokens))
      (->Program declarations)
      (let [[new-state decl] (parse-statement state)]
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

;; Парсер для указателей (устраняем леворекурсию через итерацию)
(defn parse-pointer-type [state]
  (loop [current-state state
         pointer-depth 0]
    (let [token (current-token current-state)]
      (if (= (:value token) "*")
        (recur (advance current-state) (inc pointer-depth))
        (let [[final-state base-type] (parse-type current-state)]
          [final-state (reduce (fn [type _] (->PointerType type))
                             base-type
                             (range pointer-depth))])))))

;; Парсер для массивов (устраняем леворекурсию через итерацию)
(defn parse-array-dimensions [state]
  (loop [current-state state
         dimensions []]
    (let [token (current-token current-state)]
      (if (= (:value token) "[")
        (let [state1 (advance current-state)
              [state2 size] (parse-expression state1)
              state3 (expect-token state2 :bracket "]")]
          (recur state3 (conj dimensions size)))
        [current-state dimensions]))))

(defn parse-array-type [initial-state]
  (let [[state1 base-type] (parse-type initial-state)
        [state2 dimensions] (parse-array-dimensions state1)]
    [state2 (->ArrayType base-type dimensions)]))

;; Парсер для структур
(defn parse-struct-field [state]
  (let [[state1 field-type] (parse-type state)
        [state2 field-name] (parse-identifier state1)
        state3 (expect-token state2 :separator ";")]
    [state3 (->StructField field-type (:name field-name))]))

(defn parse-struct-fields [state]
  (loop [current-state state
         fields []]
    (let [token (current-token current-state)]
      (if (= (:value token) "}")
        [current-state fields]
        (let [[new-state field] (parse-struct-field current-state)]
          (recur new-state (conj fields field)))))))

(defn parse-struct-type [state]
  (let [state1 (expect-token state :keyword "struct")
        [state2 name] (parse-identifier state1)
        state3 (expect-token state2 :bracket "{")
        [state4 fields] (parse-struct-fields state3)
        state5 (expect-token state4 :bracket "}")]
    [state5 (->StructType (:name name) fields)]))
