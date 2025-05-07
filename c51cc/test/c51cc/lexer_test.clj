(ns c51cc.lexer_test
  (:require [clojure.test :refer :all]
            [c51cc.lexer :as lexer]
            [c51cc.logger :as log]))

;; Тесты для токенизации ключевых слов
(deftest test-keyword-tokenization
  (testing "Токенизация основных типов данных"

    (is (= [{:type :void-type-keyword, :value "void"}] (lexer/tokenize "void")))
    (is (= [{:type :int-type-keyword, :value "int"}] (lexer/tokenize "int")))
    (is (= [{:type :char-type-keyword, :value "char"}] (lexer/tokenize "char")))
    (is (= [{:type :signed-type-keyword, :value "signed"}] (lexer/tokenize "signed")))
    (is (= [{:type :unsigned-type-keyword, :value "unsigned"}] (lexer/tokenize "unsigned"))))
  
  (testing "Токенизация управляющих конструкций"
    (is (= [{:type :if-control-keyword, :value "if"}] (lexer/tokenize "if")))
    (is (= [{:type :else-control-keyword, :value "else"}] (lexer/tokenize "else")))
    (is (= [{:type :for-control-keyword, :value "for"}] (lexer/tokenize "for")))
    (is (= [{:type :while-control-keyword, :value "while"}] (lexer/tokenize "while")))
    (is (= [{:type :return-control-keyword, :value "return"}] (lexer/tokenize "return")))))

;;Тест для ключевого слова main
(deftest test-main-keyword
  (testing "Токенизация ключевого слова main"
    (is (= [{:type :main-keyword, :value "main"}] (lexer/tokenize "main")))))

;; Тесты для Специальные ключевые слова микроконтроллера
(deftest test-c51-keywords
  (testing "Токенизация специальных ключевых слов микроконтроллера"
    (is (= [{:type :interrupt-c51-keyword, :value "interrupt"}] (lexer/tokenize "interrupt")))
    (is (= [{:type :sfr-c51-keyword, :value "sfr"}] (lexer/tokenize "sfr")))
    (is (= [{:type :sbit-c51-keyword, :value "sbit"}] (lexer/tokenize "sbit")))
    (is (= [{:type :using-c51-keyword, :value "using"}] (lexer/tokenize "using")))))

;; Тесты для токенизации скобок
(deftest test-bracket-tokenization
  (testing "Токенизация различных типов скобок"
    (is (= [{:type :open-round-bracket, :value "("}] (lexer/tokenize "(")))
    (is (= [{:type :close-round-bracket, :value ")"}] (lexer/tokenize ")")))
    (is (= [{:type :open-curly-bracket, :value "{"}] (lexer/tokenize "{")))
    (is (= [{:type :close-curly-bracket, :value "}"}] (lexer/tokenize "}")))
    (is (= [{:type :open-square-bracket, :value "["}] (lexer/tokenize "[")))
    (is (= [{:type :close-square-bracket, :value "]"}] (lexer/tokenize "]")))
    ;; (is (= [{:type :open-angle-bracket, :value "<"}] (lexer/tokenize "<")))
    ;; (is (= [{:type :close-angle-bracket, :value ">"}] (lexer/tokenize ">")))
    ))

;; Операторы сравнения
(deftest test-comparison-operator-tokenization
  (testing "Токенизация операторов сравнения"
    (is (= [{:type :greater-comparison-operator, :value ">"}] (lexer/tokenize ">")))
    (is (= [{:type :less-comparison-operator, :value "<"}] (lexer/tokenize "<")))
    (is (= [{:type :greater-equal-comparison-operator, :value ">="}] (lexer/tokenize ">=")))
    (is (= [{:type :less-equal-comparison-operator, :value "<="}] (lexer/tokenize "<=")))
    (is (= [{:type :not-equal-comparison-operator, :value "!="}] (lexer/tokenize "!=")))
    ))

;; Операторы присваивания
(deftest test-assignment-operator-tokenization
  (testing "Токенизация операторов присваивания"
    (is (= [{:type :equal-assignment-operator, :value "="}] (lexer/tokenize "=")))
    (is (= [{:type :and-equal-assignment-operator, :value "&="}] (lexer/tokenize "&=")))
    (is (= [{:type :or-equal-assignment-operator, :value "|="}] (lexer/tokenize "|=")))
    (is (= [{:type :xor-equal-assignment-operator, :value "^="}] (lexer/tokenize "^=")))))


;; Битовые операторы
(deftest test-bitwise-operator-tokenization
  (testing "Токенизация битовых операторов"
    (is (= [{:type :and-bitwise-operator, :value "&"}] (lexer/tokenize "&")))
    (is (= [{:type :or-bitwise-operator, :value "|"}] (lexer/tokenize "|")))
    (is (= [{:type :xor-bitwise-operator, :value "^"}] (lexer/tokenize "^")))
    (is (= [{:type :not-bitwise-operator, :value "~"}] (lexer/tokenize "~")))))

;; Разделители
(deftest test-separator-tokenization
  (testing "Токенизация разделителей"
    (is (= [{:type :semicolon-separator, :value ";"}] (lexer/tokenize ";")))
    (is (= [{:type :comma-separator, :value ","}] (lexer/tokenize ",")))
    (is (= [{:type :dot-separator, :value "."}] (lexer/tokenize ".")))
    (is (= [{:type :colon-separator, :value ":"}] (lexer/tokenize ":")))))

;; Тесты для токенизации чисел
(deftest test-number-tokenization
  (testing "Токенизация целых чисел"
    (is (= [{:type :int-number :value 42}] (lexer/tokenize "42")))
    (is (= [{:type :int-number :value 0}] (lexer/tokenize "0"))))
  
  (testing "Токенизация шестнадцатеричных чисел"
    (is (= [{:type :hex-number :value 42}] (lexer/tokenize "0x2A")))
    (is (= [{:type :hex-number :value 255}] (lexer/tokenize "0xFF")))))


;; Арифметические операторы
(deftest test-arithmetic-operator-tokenization
  (testing "Токенизация арифметических операторов"
    (is (= [{:type :plus-math-operator, :value "+"}] (lexer/tokenize "+")))
    (is (= [{:type :minus-math-operator, :value "-"}] (lexer/tokenize "-")))
    (is (= [{:type :multiply-math-operator, :value "*"}] (lexer/tokenize "*")))
    (is (= [{:type :divide-math-operator, :value "/"}] (lexer/tokenize "/")))
    (is (= [{:type :modulo-math-operator, :value "%"}] (lexer/tokenize "%"))))) 

;; Логические операторы
(deftest test-logical-operator-tokenization
  (testing "Токенизация логических операторов"
    (is (= [{:type :or-logical-operator, :value "||"}] (lexer/tokenize "||")))
    (is (= [{:type :and-logical-operator, :value "&&"}] (lexer/tokenize "&&")))
    (is (= [{:type :not-logical-operator, :value "!"}] (lexer/tokenize "!")))
    ))



;; ;; Тесты для токенизации составных выражений
;; (deftest test-complex-tokenization
;;   (testing "Токенизация простой main функции"
;;     (is (= [{:type :void-keyword, :value "void"}
;;             {:type :main-keyword, :value "main"}
;;             {:type :open-round-bracket, :value "("}
;;             {:type :close-round-bracket, :value ")"}
;;             {:type :open-curly-bracket, :value "{"}
;;             {:type :return-keyword, :value "return"}
;;             {:type :int-number, :value 0}
;;             {:type :semicolon, :value ";"}
;;             {:type :close-curly-bracket, :value "}"}]
;;            (lexer/tokenize "void main() { return 0; }")))))

;; ;; Тесты для специфических ключевых слов микроконтроллера
;; (deftest test-microcontroller-keywords
;;   (testing "Токенизация специфических ключевых слов"
;;     (is (= [{:type :interrupt-keyword, :value "interrupt"}] (lexer/tokenize "interrupt")))
;;     (is (= [{:type :sfr-keyword, :value "sfr"}] (lexer/tokenize "sfr")))
;;     (is (= [{:type :sbit-keyword, :value "sbit"}] (lexer/tokenize "sbit")))
;;     (is (= [{:type :bit-keyword, :value "bit"}] (lexer/tokenize "bit")))))

;; ;; Тесты для обработки идентификаторов
;; (deftest test-identifier-tokenization
;;   (testing "Токенизация простых идентификаторов"
;;     (is (= [{:type :identifier, :value "variable"}] 
;;            (lexer/tokenize "variable")))
;;     (is (= [{:type :identifier, :value "hello_world"}] 
;;            (lexer/tokenize "hello_world")))))

;; ;; Добавим более развернутые тесты
;; (deftest test-comprehensive-tokenization
;;   (testing "Сложные сценарии токенизации"
;;     (is (= [{:type :int-keyword, :value "int"} 
;;             {:type :identifier, :value "example_func"} 
;;             {:type :open-round-bracket, :value "("} 
;;             {:type :identifier, :value "param1"}
;;             {:type :comma, :value ","} 
;;             {:type :int-number, :value 42} 
;;             {:type :close-round-bracket, :value ")"} 
;;             {:type :semicolon, :value ";"}]
;;            (lexer/tokenize "int example_func(param1, 42);")))
    
;;     (is (= [{:type :if-keyword, :value "if"} 
;;             {:type :open-round-bracket, :value "("} 
;;             {:type :identifier, :value "x"} 
;;             {:type :greater-equal, :value ">="} 
;;             {:type :int-number, :value 10} 
;;             {:type :and-bitwise, :value "&&"} 
;;             {:type :identifier, :value "y"} 
;;             {:type :less, :value "<"} 
;;             {:type :int-number, :value 100} 
;;             {:type :close-round-bracket, :value ")"}]
;;            (lexer/tokenize "if (x >= 10 && y < 100)")))))

;; ;; Добавим тесты для обработки краевых случаев
;; (deftest test-edge-cases
;;   (testing "Обработка сложных идентификаторов и чисел"
;;     (is (= [{:type :identifier, :value "_underscore_var"}] 
;;            (lexer/tokenize "_underscore_var")))
    
;;     (is (= [{:type :hex-number, :value 255}] 
;;            (lexer/tokenize "0xFF")))
    
;;     (is (= [{:type :int-keyword, :value "int"} 
;;             {:type :identifier, :value "x"} 
;;             {:type :equal, :value "="} 
;;             {:type :hex-number, :value 15} 
;;             {:type :semicolon, :value ";"}]
;;            (lexer/tokenize "int x = 0xF;"))))
  
;;   (testing "Обработка составных операторов"
;;     (is (= [{:type :int-keyword, :value "int"} 
;;             {:type :identifier, :value "x"} 
;;             {:type :and-equal, :value "&="} 
;;             {:type :int-number, :value 10} 
;;             {:type :semicolon, :value ";"}]
;;            (lexer/tokenize "int x &= 10;")))))