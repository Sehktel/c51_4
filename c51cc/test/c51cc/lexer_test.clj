(ns c51cc.lexer_test
  (:require [clojure.test :refer :all]
            [c51cc.lexer :as lexer]
            [c51cc.logger :as log]))

;; Тесты для токенизации ключевых слов
(deftest test-keyword-tokenization
  (testing "Токенизация основных типов данных"
    (is (= [{:type :keyword, :value "int"}] (lexer/tokenize "int")))
    (is (= [{:type :keyword, :value "char"}] (lexer/tokenize "char")))
    (is (= [{:type :keyword, :value "void"}] (lexer/tokenize "void"))))
  
  (testing "Токенизация управляющих конструкций"
    (is (= [{:type :keyword, :value "if"}] (lexer/tokenize "if")))
    (is (= [{:type :keyword, :value "else"}] (lexer/tokenize "else")))
    (is (= [{:type :keyword, :value "for"}] (lexer/tokenize "for")))
    (is (= [{:type :keyword, :value "while"}] (lexer/tokenize "while")))
    (is (= [{:type :keyword, :value "return"}] (lexer/tokenize "return")))))

;;Тест для ключевого слова main
(deftest test-main-keyword
  (testing "Токенизация ключевого слова main"
    (is (= [{:type :keyword, :value "main"}] (lexer/tokenize "main")))))


;; Тесты для токенизации скобок
(deftest test-bracket-tokenization
  (testing "Токенизация различных типов скобок"
    (is (= [{:type :bracket, :value "("}] (lexer/tokenize "(")))
    (is (= [{:type :bracket, :value ")"}] (lexer/tokenize ")")))
    (is (= [{:type :bracket, :value "{"}] (lexer/tokenize "{")))
    (is (= [{:type :bracket, :value "}"}] (lexer/tokenize "}")))
    (is (= [{:type :bracket, :value "["}] (lexer/tokenize "[")))
    (is (= [{:type :bracket, :value "]"}] (lexer/tokenize "]")))))

;; Тесты для токенизации чисел
(deftest test-number-tokenization
  (testing "Токенизация целых чисел"
    (is (= [{:type :int-number :value 42}] (lexer/tokenize "42")))
    (is (= [{:type :int-number :value 0}] (lexer/tokenize "0"))))
  
  (testing "Токенизация шестнадцатеричных чисел"
    (is (= [{:type :hex-number :value 42}] (lexer/tokenize "0x2A")))
    (is (= [{:type :hex-number :value 255}] (lexer/tokenize "0xFF")))))

;; Тесты для токенизации составных выражений
(deftest test-complex-tokenization
  (testing "Токенизация простой main функции"
    (is (= [{:type :keyword, :value "void"}
            {:type :keyword, :value "main"}
            {:type :bracket, :value "("}
            {:type :bracket, :value ")"}
            {:type :bracket, :value "{"}
            {:type :keyword, :value "return"}
            {:type :int-number, :value 0}
            {:type :operator, :value ";"}
            {:type :bracket, :value "}"}]
           (lexer/tokenize "void main() { return 0; }")))))

;; Тесты для специфических ключевых слов микроконтроллера
(deftest test-microcontroller-keywords
  (testing "Токенизация специфических ключевых слов"
    (is (= [{:type :keyword, :value "interrupt"}] (lexer/tokenize "interrupt")))
    (is (= [{:type :keyword, :value "sfr"}] (lexer/tokenize "sfr")))
    (is (= [{:type :keyword, :value "sbit"}] (lexer/tokenize "sbit")))
    (is (= [{:type :keyword, :value "bit"}] (lexer/tokenize "bit")))))

;; Тесты для обработки идентификаторов
(deftest test-identifier-tokenization
  (testing "Токенизация простых идентификаторов"
    (is (= [{:type :identifier, :value "variable"}] 
           (lexer/tokenize "variable")))
    (is (= [{:type :identifier, :value "hello_world"}] 
           (lexer/tokenize "hello_world")))))

;; Добавим более развернутые тесты
(deftest test-comprehensive-tokenization
  (testing "Сложные сценарии токенизации"
    (is (= [{:type :keyword, :value "int"} 
            {:type :identifier, :value "example_func"} 
            {:type :bracket, :value "("} 
            {:type :identifier, :value "param1"}
            {:type :operator, :value ","} 
            {:type :int-number, :value 42} 
            {:type :bracket, :value ")"} 
            {:type :operator, :value ";"}]
           (lexer/tokenize "int example_func(param1, 42);")))
    
    (is (= [{:type :keyword, :value "if"} 
            {:type :bracket, :value "("} 
            {:type :identifier, :value "x"} 
            {:type :operator, :value ">="} 
            {:type :int-number, :value 10} 
            {:type :operator, :value "&&"} 
            {:type :identifier, :value "y"} 
            {:type :operator, :value "<"} 
            {:type :int-number, :value 100} 
            {:type :bracket, :value ")"}]
           (lexer/tokenize "if (x >= 10 && y < 100)")))))

;; Добавим тесты для обработки краевых случаев
(deftest test-edge-cases
  (testing "Обработка сложных идентификаторов и чисел"
    (is (= [{:type :identifier, :value "_underscore_var"}] 
           (lexer/tokenize "_underscore_var")))
    
    (is (= [{:type :hex-number, :value 255}] 
           (lexer/tokenize "0xFF")))
    
    (is (= [{:type :keyword, :value "int"} 
            {:type :identifier, :value "x"} 
            {:type :operator, :value "="} 
            {:type :hex-number, :value 15} 
            {:type :semicolon, :value ";"}]
           (lexer/tokenize "int x = 0xF;"))))
  
  (testing "Обработка составных операторов"
    (is (= [{:type :keyword, :value "int"} 
            {:type :identifier, :value "x"} 
            {:type :operator, :value "&="} 
            {:type :int-number, :value 10} 
            {:type :semicolon, :value ";"}]
           (lexer/tokenize "int x &= 10;")))))




;; Запуск всех тестов
(run-tests)
