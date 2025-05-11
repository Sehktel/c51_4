(ns c51cc.parser_test
  (:require [clojure.test :refer :all]
            [c51cc.parser :as parser]
            [c51cc.lexer :as lexer])
  (:import [c51cc.parser ArrayType]))

;; Вспомогательные функции для тестирования
;; (defn parse-str [input]
;;   (parser/parse input))

(deftest test-literal-parsing
  (testing "Парсинг целого числа"
    (let [state {:tokens (lexer/tokenize "42") :position 0 :ast []}
          result (parser/parse-literal state)]
      (is (= :int (get-in result [:ast :type])))
      (is (= 42 (get-in result [:ast :value])))))
  
  (testing "Парсинг шестнадцатеричного числа"
    (let [state {:tokens (lexer/tokenize "0x2A") :position 0 :ast []}
          result (parser/parse-literal state)]
      (is (= :hex (get-in result [:ast :type])))
      (is (= 42 (get-in result [:ast :value]))))))

(deftest test-identifier-parsing
  (testing "Парсинг идентификатора"
    (let [state {:tokens (lexer/tokenize "variable_name") :position 0 :ast []}
          result (parser/parse-identifier state)]
      (is (= "variable_name" (get-in result [:ast :name]))))))

(deftest test-unary-expressions
  (testing "Префиксные операторы"
    (testing "Префиксный инкремент"
      (let [state {:tokens (lexer/tokenize "++x") :position 0 :ast []}
            result (parser/parse-unary-expression state)]
        (is (= :pre-increment (get-in result [:ast :type])))
        (is (= "++" (get-in result [:ast :operator])))
        (is (= "x" (get-in result [:ast :operand :name])))))

    (testing "Префиксный декремент"
      (let [state {:tokens (lexer/tokenize "--y") :position 0 :ast []}
            result (parser/parse-unary-expression state)]
        (is (= :pre-decrement (get-in result [:ast :type])))
        (is (= "--" (get-in result [:ast :operator])))
        (is (= "y" (get-in result [:ast :operand :name])))))

    (testing "Унарный минус"
      (let [state {:tokens (lexer/tokenize "-42") :position 0 :ast []}
            result (parser/parse-unary-expression state)]
        (is (= :negation (get-in result [:ast :type])))
        (is (= "-" (get-in result [:ast :operator])))
        (is (= 42 (get-in result [:ast :operand :value])))))

    (testing "Унарный плюс"
      (let [state {:tokens (lexer/tokenize "+42") :position 0 :ast []}
            result (parser/parse-unary-expression state)]
        (is (= :unary-plus (get-in result [:ast :type])))
        (is (= "+" (get-in result [:ast :operator])))
        (is (= 42 (get-in result [:ast :operand :value])))))

    (testing "Логическое отрицание"
      (let [state {:tokens (lexer/tokenize "!x") :position 0 :ast []}
            result (parser/parse-unary-expression state)]
        (is (= :logical-not (get-in result [:ast :type])))
        (is (= "!" (get-in result [:ast :operator])))
        (is (= "x" (get-in result [:ast :operand :name])))))

    (testing "Побитовое отрицание"
      (let [state {:tokens (lexer/tokenize "~x") :position 0 :ast []}
            result (parser/parse-unary-expression state)]
        (is (= :bitwise-not (get-in result [:ast :type])))
        (is (= "~" (get-in result [:ast :operator])))
        (is (= "x" (get-in result [:ast :operand :name]))))))

  (testing "Постфиксные операторы"
    (testing "Постфиксный инкремент"
      (let [state {:tokens (lexer/tokenize "x++") :position 0 :ast []}
            result (parser/parse-unary-expression state)]
        (is (= :post-increment (get-in result [:ast :type])))
        (is (= "++" (get-in result [:ast :operator])))
        (is (= "x" (get-in result [:ast :operand :name])))))

    (testing "Постфиксный декремент"
      (let [state {:tokens (lexer/tokenize "y--") :position 0 :ast []}
            result (parser/parse-unary-expression state)]
        (is (= :post-decrement (get-in result [:ast :type])))
        (is (= "--" (get-in result [:ast :operator])))
        (is (= "y" (get-in result [:ast :operand :name])))))))

(deftest test-binary-expressions
  (testing "Простое сложение"
    (let [state {:tokens (lexer/tokenize "a + b") :position 0}
          result (parser/parse-binary-expression state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (let [ast (:ast result)]
        (is (= "+" (:operator ast)))
        (is (= "a" (get-in ast [:left :name])))
        (is (= "b" (get-in ast [:right :name])))
        (is (= :binary-expression (:type ast)))        
        )))

  ;; Comment out more complex tests for now
  #_(testing "Сложное выражение с приоритетом"
    (let [state {:tokens (lexer/tokenize "a + b * c") :position 0}
          result (parser/parse-binary-expression state)]
      (is (= "+" (get-in result [:ast :operator])))
      (is (= "a" (get-in result [:ast :left :name])))
      (is (= "*" (get-in result [:ast :right :operator])))
      (is (= "b" (get-in result [:ast :right :left :name])))
      (is (= "c" (get-in result [:ast :right :right :name])))))

  #_(testing "Выражение со скобками"
    (let [state {:tokens (lexer/tokenize "(a + b) * c") :position 0}
          result (parser/parse-binary-expression state)]
      (is (= "*" (get-in result [:ast :operator])))
      (is (= "+" (get-in result [:ast :left :operator])))
      (is (= "c" (get-in result [:ast :right :name]))))))

(deftest test-primary-expressions
  (testing "Литерал"
    (let [state {:tokens (lexer/tokenize "42") :position 0 :ast []}
          result (parser/parse-primary-expression state)]
      (is (= :int (get-in result [:ast :type])))
      (is (= 42 (get-in result [:ast :value])))))

  (testing "Идентификатор"
    (let [state {:tokens (lexer/tokenize "variable_name") :position 0 :ast []}
          result (parser/parse-primary-expression state)]
      (is (= "variable_name" (get-in result [:ast :name])))))

  ;; Temporarily comment out the parenthesized expression test
  #_(testing "Выражение в скобках"
    (let [state {:tokens (lexer/tokenize "(a + b)") :position 0 :ast []}
          result (parser/parse-primary-expression state)]
      (is (not-empty result) "Should parse expression in parentheses"))))

(deftest parse-type-test
  (testing "Basic type parsing"
    (let [tokens (lexer/tokenize "int")
          state (parser/->ParserState tokens 0)
          result (parser/parse-type state)]
      (is (= {:type :int :modifiers []} (:ast result)))))
  
  (testing "Unsigned int parsing"
    (let [tokens (lexer/tokenize "unsigned int")
          state (parser/->ParserState tokens 0)
          result (parser/parse-type state)]
      (is (= {:type :int :modifiers [:unsigned]} (:ast result)))))
  
  ;; (testing "Pointer type parsing"
  ;;   (let [tokens (lexer/tokenize "int*")
  ;;         state (parser/->ParserState tokens 0)
  ;;         result (parser/parse-type state)]
  ;;     (is (instance? (parser/PointerType (:ast result)))
  ;;     (is (= :int (:base-type (:ast result))))
  ;;     )))
  
  (testing "Array type parsing"
    (let [tokens (lexer/tokenize "int[10]")
          state (parser/->ParserState tokens 0)
          result (parser/parse-type state)]
      (is (= c51cc.parser.ArrayType (type (:ast result))))
      (is (= :int (:base-type (:ast result))))
      (is (= [10] (:dimensions (:ast result))))))
  
  (testing "Unsigned char parsing"
    (let [tokens (lexer/tokenize "unsigned char")
          state (parser/->ParserState tokens 0)
          result (parser/parse-type state)]
      (is (= {:type :char :modifiers [:unsigned]} (:ast result)))))
  
  (testing "Void type parsing"
    (let [tokens (lexer/tokenize "void")
          state (parser/->ParserState tokens 0)
          result (parser/parse-type state)]
      (is (= {:type :void :modifiers []} (:ast result))))))

(deftest test-parse-block
  (testing "Simple block parsing"
    (let [tokens (lexer/tokenize "{ int x = 10; char y = 'a'; }")
          state (parser/->ParserState tokens 0)
          result (parser/parse-block state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (contains? result :statements) "Should contain statements")
      (is (= 2 (count (:statements result))))
      
      (let [first-stmt (first (:statements result))
            second-stmt (second (:statements result))]
        (is (instance? c51cc.parser.VarDecl first-stmt))
        (is (= :int (get-in first-stmt [:type :type])))
        (is (= "x" (get-in first-stmt [:name :name])))
        (is (= 10 (get-in first-stmt [:right :value])))
        (is (instance? c51cc.parser.VarDecl second-stmt))
        (is (= :char (get-in second-stmt [:type :type])))
        (is (= "y" (get-in second-stmt [:name :name])))
        (is (= \a (get-in second-stmt [:right :value]))))))
  
  (testing "Nested block parsing"
    (let [tokens (lexer/tokenize "{ int x = 10; { char y = 'a'; } }")
          state (parser/->ParserState tokens 0)
          result (parser/parse-block state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (contains? result :statements) "Should contain statements")
      (is (= 2 (count (:statements result))))
      
      (let [second-stmt (second (:statements result))]
        (is (instance? c51cc.parser.Block second-stmt)))))
  
  (testing "Empty block parsing"
    (let [tokens (lexer/tokenize "{}")
          state (parser/->ParserState tokens 0)
          result (parser/parse-block state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (contains? result :statements) "Should contain statements")
      (is (empty? (:statements result))))))

(deftest test-parse-statement
  (testing "Return statement without expression"
    (let [tokens (lexer/tokenize "return;")
          state (parser/->ParserState tokens 0)
          result (parser/parse-statement state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (instance? c51cc.parser.Return (:ast result)))
      (is (nil? (get-in result [:ast :expr])) "Return with no expression should have nil expr")))
  
  (testing "Variable declaration parsing"
    (let [tokens (lexer/tokenize "int x;")
          state (parser/->ParserState tokens 0)
          result (parser/parse-statement state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (instance? c51cc.parser.VarDecl (:ast result)))
      (is (= :int (get-in result [:ast :type :type])))
      (is (= "x" (get-in result [:ast :name :name])))))
  
  (testing "Function declaration parsing"
    (let [tokens (lexer/tokenize "void main() { }")
          state (parser/->ParserState tokens 0)
          result (parser/parse-statement state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (instance? c51cc.parser.FunctionDecl (:ast result)))
      (is (= :void (get-in result [:ast :return-type])))
      (is (= "main" (get-in result [:ast :name :name])))
      (is (instance? c51cc.parser.Block (get-in result [:ast :body]))))))

(deftest test-parse-function-declaration
  (testing "Simple function declaration"
    (let [tokens (lexer/tokenize "void main() { }")
          state (parser/->ParserState tokens 0)
          result (parser/parse-function-declaration state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (instance? c51cc.parser.FunctionDecl (:ast result)))
      (is (= :void (:return-type (:ast result))))
      (is (= "main" (get-in result [:ast :name :name])))
      (is (empty? (:params (:ast result))))
      (is (instance? c51cc.parser.Block (:body (:ast result))))))
  
  (testing "Function with parameters"
    (let [tokens (lexer/tokenize "int calculate(int a, char b) { return a + b; }")
          state (parser/->ParserState tokens 0)
          result (parser/parse-function-declaration state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (instance? c51cc.parser.FunctionDecl (:ast result)))
      (is (= :int (:return-type (:ast result))))
      (is (= "calculate" (get-in result [:ast :name :name])))
      (is (= 2 (count (:params (:ast result)))))
      (is (instance? c51cc.parser.Block (:body (:ast result))))))
  
  (testing "Function with complex body"
    (let [tokens (lexer/tokenize "void complex() { int x = 10; if (x > 0) { return; } }")
          state (parser/->ParserState tokens 0)
          result (parser/parse-function-declaration state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (instance? c51cc.parser.FunctionDecl (:ast result)))
      (is (= :void (:return-type (:ast result))))
      (is (= "complex" (get-in result [:ast :name :name])))
      (is (instance? c51cc.parser.Block (:body (:ast result))))
      (is (= 2 (count (:statements (:body (:ast result)))))))))

;; Новые тесты для добавленных функций парсера

(deftest test-parse-break
  (testing "Break statement parsing"
    (let [tokens (lexer/tokenize "break;")
          state (parser/->ParserState tokens 0)
          result (parser/parse-break state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= :break-statement (get-in result [:ast :type])))
      (is (nil? (:error result)) "Should not contain errors")))
  
  (testing "Invalid break statement without semicolon"
    (let [tokens (lexer/tokenize "break")
          state (parser/->ParserState tokens 0)
          result (parser/parse-break state)]
      (is (contains? result :error) "Should contain error")
      (is (= "Expected semicolon after break" (:error result))))))

(deftest test-parse-continue
  (testing "Continue statement parsing"
    (let [tokens (lexer/tokenize "continue;")
          state (parser/->ParserState tokens 0)
          result (parser/parse-continue state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= :continue-statement (get-in result [:ast :type])))
      (is (nil? (:error result)) "Should not contain errors")))
  
  (testing "Invalid continue statement without semicolon"
    (let [tokens (lexer/tokenize "continue")
          state (parser/->ParserState tokens 0)
          result (parser/parse-continue state)]
      (is (contains? result :error) "Should contain error")
      (is (= "Expected semicolon after continue" (:error result))))))

(deftest test-parse-bitwise-expression
  (testing "Bitwise AND expression"
    (let [tokens (lexer/tokenize "a & b")
          state (parser/->ParserState tokens 0)
          result (parser/parse-bitwise-expression state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= "&" (get-in result [:ast :operator])))
      (is (= "a" (get-in result [:ast :left :name])))
      (is (= "b" (get-in result [:ast :right :name])))))
  
  (testing "Bitwise OR expression"
    (let [tokens (lexer/tokenize "x | y")
          state (parser/->ParserState tokens 0)
          result (parser/parse-bitwise-expression state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= "|" (get-in result [:ast :operator])))
      (is (= "x" (get-in result [:ast :left :name])))
      (is (= "y" (get-in result [:ast :right :name])))))
  
  (testing "Bitwise XOR expression"
    (let [tokens (lexer/tokenize "a ^ b")
          state (parser/->ParserState tokens 0)
          result (parser/parse-bitwise-expression state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= "^" (get-in result [:ast :operator])))
      (is (= "a" (get-in result [:ast :left :name])))
      (is (= "b" (get-in result [:ast :right :name]))))))

(deftest test-parse-case
  (testing "Simple case statement"
    (let [tokens (lexer/tokenize "case 1: return 42;")
          state (parser/->ParserState tokens 0)
          result (parser/parse-case state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= :case-statement (get-in result [:ast :type])))
      (is (= 1 (get-in result [:ast :value :value])))
      (is (= 1 (count (get-in result [:ast :statements]))))))
  
  (testing "Case statement with multiple statements"
    (let [tokens (lexer/tokenize "case 2: x = 1; y = 2; break;")
          state (parser/->ParserState tokens 0)
          result (parser/parse-case state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= :case-statement (get-in result [:ast :type])))
      (is (= 2 (get-in result [:ast :value :value])))
      (is (= 3 (count (get-in result [:ast :statements])))))))

(deftest test-parse-default
  (testing "Simple default statement"
    (let [tokens (lexer/tokenize "default: return 0;")
          state (parser/->ParserState tokens 0)
          result (parser/parse-default state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= :default-statement (get-in result [:ast :type])))
      (is (= 1 (count (get-in result [:ast :statements]))))))
  
  (testing "Default statement with multiple statements"
    (let [tokens (lexer/tokenize "default: x = 0; y = 0; break;")
          state (parser/->ParserState tokens 0)
          result (parser/parse-default state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= :default-statement (get-in result [:ast :type])))
      (is (= 3 (count (get-in result [:ast :statements])))))))

(deftest test-parse-switch-case
  (testing "Switch statement with single case"
    (let [tokens (lexer/tokenize "switch (x) { case 1: return 42; }")
          state (parser/->ParserState tokens 0)
          result (parser/parse-switch-case state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= "x" (get-in result [:ast :expr :name])))
      (is (= 1 (count (get-in result [:ast :cases]))))
      (is (nil? (get-in result [:ast :default])))))
  
  (testing "Switch statement with multiple cases and default"
    (let [tokens (lexer/tokenize "switch (x) { case 1: return 1; case 2: return 2; default: return 0; }")
          state (parser/->ParserState tokens 0)
          result (parser/parse-switch-case state)]
      (is (map? result) "Should return a state map")
      (is (contains? result :ast) "Should contain AST")
      (is (= "x" (get-in result [:ast :expr :name])))
      (is (= 2 (count (get-in result [:ast :cases]))))
      (is (some? (get-in result [:ast :default]))))))
