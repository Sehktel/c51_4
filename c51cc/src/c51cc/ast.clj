(ns c51cc.ast
  (:require [c51cc.logger :as log]
            [c51cc.ast.nodes :as nodes]
            [c51cc.ast.visitors :as visitors]))

(defprotocol ASTNode
  "Протокол для реализации паттерна посетителя AST"
  (accept [this visitor]))

(extend-protocol ASTNode
  c51cc.ast.nodes.Program
  (accept [this visitor]
    (visitors/visit-program visitor this))

  c51cc.ast.nodes.FunctionDeclaration
  (accept [this visitor]
    (visitors/visit-function-declaration visitor this))

  c51cc.ast.nodes.VariableDeclaration
  (accept [this visitor]
    (visitors/visit-variable-declaration visitor this))

  c51cc.ast.nodes.Assignment
  (accept [this visitor]
    (visitors/visit-assignment visitor this))

  c51cc.ast.nodes.Block
  (accept [this visitor]
    (visitors/visit-block visitor this))

  c51cc.ast.nodes.ReturnStatement
  (accept [this visitor]
    (visitors/visit-return-statement visitor this))

  c51cc.ast.nodes.BinaryExpression
  (accept [this visitor]
    (visitors/visit-binary-expression visitor this))

  c51cc.ast.nodes.UnaryExpression
  (accept [this visitor]
    (visitors/visit-unary-expression visitor this))

  c51cc.ast.nodes.Literal
  (accept [this visitor]
    (visitors/visit-literal visitor this))

  c51cc.ast.nodes.Identifier
  (accept [this visitor]
    (visitors/visit-identifier visitor this))

  c51cc.ast.nodes.SfrDeclaration
  (accept [this visitor]
    (visitors/visit-sfr-declaration visitor this))

  c51cc.ast.nodes.SbitDeclaration
  (accept [this visitor]
    (visitors/visit-sbit-declaration visitor this))

  c51cc.ast.nodes.ArrayType
  (accept [this visitor]
    (visitors/visit-array-type visitor this))

  c51cc.ast.nodes.ForLoop
  (accept [this visitor]
    (visitors/visit-for-loop visitor this))

  c51cc.ast.nodes.WhileLoop
  (accept [this visitor]
    (visitors/visit-while-loop visitor this))

  c51cc.ast.nodes.InterruptDeclaration
  (accept [this visitor]
    (visitors/visit-interrupt-declaration visitor this))

  c51cc.ast.nodes.UsingDeclaration
  (accept [this visitor]
    (visitors/visit-using-declaration visitor this)))

;; Функция для красивой печати AST
(defn pretty-print [ast]
  (visitors/pretty-print-ast ast))
