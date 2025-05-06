(ns c51cc.lexer_test
  (:require [clojure.test :refer :all]
            [c51cc.lexer :as lexer]))

;; Тесты для токенизации ключевых слов
(deftest test-keyword-tokenization
  (testing "Токенизация основных типов данных"
    (is (= [:int] (lexer/tokenize "int")))
    (is (= [:char] (lexer/tokenize "char")))
    (is (= [:void] (lexer/tokenize "void"))))
  
  (testing "Токенизация управляющих конструкций"
    (is (= [:if] (lexer/tokenize "if")))
    (is (= [:else] (lexer/tokenize "else")))
    (is (= [:for] (lexer/tokenize "for")))
    (is (= [:while] (lexer/tokenize "while")))
    (is (= [:return] (lexer/tokenize "return")))))

;;Тест для ключевого слова main
(deftest test-main-keyword
  (testing "Токенизация ключевого слова main"
    (is (= [:main] (lexer/tokenize "main")))))


;; Тесты для токенизации скобок
(deftest test-bracket-tokenization
  (testing "Токенизация различных типов скобок"
    (is (= [:open-round-bracket] (lexer/tokenize "(")))
    (is (= [:close-round-bracket] (lexer/tokenize ")")))
    (is (= [:open-curly-bracket] (lexer/tokenize "{")))
    (is (= [:close-curly-bracket] (lexer/tokenize "}")))
    (is (= [:open-square-bracket] (lexer/tokenize "[")))
    (is (= [:close-square-bracket] (lexer/tokenize "]")))))

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
    (is (= [:void
            :main 
            :open-round-bracket 
            :close-round-bracket 
            :open-curly-bracket 
            :return 
            {:type :int-number :value 0} 
            :semicolon 
            :close-curly-bracket]
           (lexer/tokenize "void main() { return 0; }"))))

  (testing "Токенизация простой функции"
    (is (= [:int 
            {:type :identifier :value "main"} 
            :open-round-bracket 
            :close-round-bracket 
            :open-curly-bracket 
            :return 
            {:type :int-number :value 42} 
            :semicolon 
            :close-curly-bracket] 
           (lexer/tokenize "int main() { return 42; }"))))
  
  (testing "Токенизация выражений с операторами"
    (is (= [:int 
            {:type :identifier :value "x"} 
            :equal 
            {:type :int-number :value 10} 
            :semicolon]
           (lexer/tokenize "int x = 10;")))
    
    (is (= [:if 
            :open-round-bracket 
            {:type :identifier :value "x"} 
            :greater 
            {:type :int-number :value 0} 
            :close-round-bracket]
           (lexer/tokenize "if (x > 0)")))))

;; Тесты для специфических ключевых слов микроконтроллера
(deftest test-microcontroller-keywords
  (testing "Токенизация специфических ключевых слов"
    (is (= [:interrupt] (lexer/tokenize "interrupt")))
    (is (= [:sfr] (lexer/tokenize "sfr")))
    (is (= [:sbit] (lexer/tokenize "sbit")))
    (is (= [:bit] (lexer/tokenize "bit")))))

;; Тесты для обработки идентификаторов
(deftest test-identifier-tokenization
  (testing "Токенизация простых идентификаторов"
    (is (= [{:type :identifier :value "variable"}] 
           (lexer/tokenize "variable")))
    (is (= [{:type :identifier :value "hello_world"}] 
           (lexer/tokenize "hello_world")))))

;; Добавим более развернутые тесты
(deftest test-comprehensive-tokenization
  (testing "Сложные сценарии токенизации"
    (is (= [:int 
            {:type :identifier :value "example_func"} 
            :open-round-bracket 
            {:type :identifier :value "param1"}
            :comma 
            {:type :int-number :value 42} 
            :close-round-bracket 
            :semicolon]
           (lexer/tokenize "int example_func(param1, 42);")))
    
    (is (= [:if 
            :open-round-bracket 
            {:type :identifier :value "x"} 
            :greater-equal 
            {:type :int-number :value 10} 
            :and 
            {:type :identifier :value "y"} 
            :less 
            {:type :int-number :value 100} 
            :close-round-bracket]
           (lexer/tokenize "if (x >= 10 && y < 100)")))))

;; Добавим тесты для обработки краевых случаев
(deftest test-edge-cases
  (testing "Обработка сложных идентификаторов и чисел"
    (is (= [{:type :identifier :value "_underscore_var"}] 
           (lexer/tokenize "_underscore_var")))
    
    (is (= [{:type :hex-number :value 255}] 
           (lexer/tokenize "0xFF")))
    
    (is (= [:int {:type :identifier :value "x"} :equal {:type :hex-number :value 15} :semicolon]
           (lexer/tokenize "int x = 0xF;"))))
  
  (testing "Обработка составных операторов"
    (is (= [:int {:type :identifier :value "x"} :and-equal {:type :int-number :value 10} :semicolon]
           (lexer/tokenize "int x &= 10;")))))




;; Запуск всех тестов
(run-tests)
