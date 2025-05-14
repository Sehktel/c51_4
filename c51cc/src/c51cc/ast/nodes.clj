(ns c51cc.ast.nodes
  "Пространство имен для определения узлов AST.
   Содержит все типы узлов, используемые в AST.")

;; Программа содержит список объявлений
(defrecord Program [declarations])

;; Объявление функции содержит тип возвращаемого значения, имя, параметры, 
;; вектор прерываний, вектор использования, тело
(defrecord FunctionDeclaration [return-type name params interrupt using body])

;; Объявление переменной содержит тип, имя
(defrecord VariableDeclaration [type name])

;; Оператор присваивания содержит левую и правую часть
(defrecord Assignment [left right operator])

;; Блок содержит список операторов
(defrecord Block [statements])

;; Оператор возврата содержит выражение
(defrecord ReturnStatement [expr])

;; Бинарное выражение содержит оператор и два операнда
(defrecord BinaryExpression [operator left right])

;; Унарное выражение содержит оператор и один операнд
(defrecord UnaryExpression [operator operand])

;; Литерал содержит тип и значение
(defrecord Literal [type value])

;; Идентификатор содержит имя
(defrecord Identifier [name])

;; Объявление SFR содержит имя и адрес
(defrecord SfrDeclaration [name address])

;; Объявление SBIT содержит имя и бит
(defrecord SbitDeclaration [name bit])

;; Массив содержит базовый тип и размеры
(defrecord ArrayType [base-type dimensions])

;; Цикл for содержит начальное значение, конечное значение, шаг, тело
(defrecord ForLoop [init end step body])

;; Цикл while содержит условие, тело
(defrecord WhileLoop [condition body])

;; Объявление прерывания содержит номер и функцию
(defrecord InterruptDeclaration [number func])

;; Объявление using содержит имя и список переменных
(defrecord UsingDeclaration [name vars]) 