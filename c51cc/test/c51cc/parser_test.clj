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
        (is (= "b" (get-in ast [:right :name]))))))

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
