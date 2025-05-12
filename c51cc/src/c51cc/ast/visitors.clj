(ns c51cc.ast.visitors
  "Пространство имен для реализации паттерна посетителя AST.
   Содержит протокол и реализации различных обходов AST."
  (:require [c51cc.logger :as log]
            [clojure.string :as str]))

(declare visit-any)  ;; Предварительное объявление функции

(defprotocol ASTVisitor
  "Протокол для реализации паттерна посетителя AST.
   Каждый метод принимает узел AST и возвращает результат обхода."
  (visit-program [this node] "Посещает корневой узел программы")
  (visit-function-declaration [this node] "Посещает объявление функции")
  (visit-variable-declaration [this node] "Посещает объявление переменной")
  (visit-assignment [this node] "Посещает оператор присваивания")
  (visit-block [this node] "Посещает блок кода")
  (visit-return-statement [this node] "Посещает оператор return")
  (visit-binary-expression [this node] "Посещает бинарное выражение")
  (visit-unary-expression [this node] "Посещает унарное выражение")
  (visit-literal [this node] "Посещает литерал")
  (visit-identifier [this node] "Посещает идентификатор")
  (visit-sfr-declaration [this node] "Посещает объявление SFR")
  (visit-sbit-declaration [this node] "Посещает объявление SBIT")
  (visit-array-type [this node] "Посещает тип массива")
  (visit-for-loop [this node] "Посещает цикл for")
  (visit-while-loop [this node] "Посещает цикл while")
  (visit-interrupt-declaration [this node] "Посещает объявление прерывания")
  (visit-using-declaration [this node] "Посещает объявление using"))

;; Реализация посетителя для красивой печати AST
(defrecord PrettyPrintVisitor [indent]
  ASTVisitor
  (visit-program [this node]
    (let [declarations (:declarations node)]
      (str "Program:\n"
           (str/join "\n" (map #(visit-any this % (inc indent)) declarations)))))
  
  (visit-function-declaration [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "Function " (:name node) ":\n"
           indent-str "  Return type: " (:return-type node) "\n"
           indent-str "  Parameters: " (str/join ", " (:params node)) "\n"
           (when (:interrupt node)
             (str indent-str "  Interrupt: " (visit-any this (:interrupt node) (+ indent 2)) "\n"))
           (when (:using node)
             (str indent-str "  Using: " (visit-any this (:using node) (+ indent 2)) "\n"))
           indent-str "  Body:\n"
           (visit-any this (:body node) (+ indent 2)))))
  
  (visit-variable-declaration [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "Variable " (:name node) " : " (:type node))))
  
  (visit-block [this node]
    (let [indent-str (apply str (repeat indent "  "))
          statements (:statements node)]
      (str indent-str "Block:\n"
           (str/join "\n" (map #(visit-any this % (inc indent)) statements)))))
  
  (visit-assignment [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str (visit-any this (:left node) 0)
           " = "
           (visit-any this (:right node) 0))))
  
  (visit-return-statement [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "Return: " (visit-any this (:expr node) (inc indent)))))
  
  (visit-binary-expression [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "(" (visit-any this (:left node) 0)
           " " (:operator node) " "
           (visit-any this (:right node) 0) ")")))
  
  (visit-unary-expression [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str (:operator node)
           (visit-any this (:operand node) 0))))
  
  (visit-literal [this node]
    (str (:value node)))
  
  (visit-identifier [this node]
    (str (:name node)))
  
  (visit-sfr-declaration [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "SFR " (:name node) " at " (:address node))))
  
  (visit-sbit-declaration [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "SBIT " (:name node) " at bit " (:bit node))))
  
  (visit-array-type [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "Array of " (:base-type node)
           " with dimensions " (str/join "x" (:dimensions node)))))
  
  (visit-for-loop [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "For:\n"
           indent-str "  Init: " (visit-any this (:init node) (+ indent 2)) "\n"
           indent-str "  End: " (visit-any this (:end node) (+ indent 2)) "\n"
           indent-str "  Step: " (visit-any this (:step node) (+ indent 2)) "\n"
           indent-str "  Body:\n"
           (visit-any this (:body node) (+ indent 2)))))
  
  (visit-while-loop [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "While:\n"
           indent-str "  Condition: " (visit-any this (:condition node) (+ indent 2)) "\n"
           indent-str "  Body:\n"
           (visit-any this (:body node) (+ indent 2)))))
  
  (visit-interrupt-declaration [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "Interrupt " (:number node) " -> " (:func node))))
  
  (visit-using-declaration [this node]
    (let [indent-str (apply str (repeat indent "  "))]
      (str indent-str "Using " (:name node) " (" (str/join ", " (:vars node)) ")"))))

(defn visit-any
  "Вспомогательная функция для посещения любого узла AST"
  [visitor node indent]
  (cond
    (nil? node) ""
    :else
    (condp = (type node)
      c51cc.ast.nodes.Program (visit-program visitor node)
      c51cc.ast.nodes.FunctionDeclaration (visit-function-declaration visitor node)
      c51cc.ast.nodes.VariableDeclaration (visit-variable-declaration visitor node)
      c51cc.ast.nodes.Assignment (visit-assignment visitor node)
      c51cc.ast.nodes.Block (visit-block visitor node)
      c51cc.ast.nodes.ReturnStatement (visit-return-statement visitor node)
      c51cc.ast.nodes.BinaryExpression (visit-binary-expression visitor node)
      c51cc.ast.nodes.UnaryExpression (visit-unary-expression visitor node)
      c51cc.ast.nodes.Literal (visit-literal visitor node)
      c51cc.ast.nodes.Identifier (visit-identifier visitor node)
      c51cc.ast.nodes.SfrDeclaration (visit-sfr-declaration visitor node)
      c51cc.ast.nodes.SbitDeclaration (visit-sbit-declaration visitor node)
      c51cc.ast.nodes.ArrayType (visit-array-type visitor node)
      c51cc.ast.nodes.ForLoop (visit-for-loop visitor node)
      c51cc.ast.nodes.WhileLoop (visit-while-loop visitor node)
      c51cc.ast.nodes.InterruptDeclaration (visit-interrupt-declaration visitor node)
      c51cc.ast.nodes.UsingDeclaration (visit-using-declaration visitor node)
      (str node))))

(defn pretty-print-ast
  "Красиво печатает AST с отступами"
  [ast]
  (let [visitor (->PrettyPrintVisitor 0)
        result (visit-any visitor ast 0)]
    (log/trace result)
    result))
