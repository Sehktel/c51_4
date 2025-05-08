(ns c51cc.parser_test
  (:require [clojure.test :refer :all]
            [c51cc.parser :as parser]
            [c51cc.lexer :as lexer]))

;; Вспомогательные функции для тестирования
(defn parse-str [input]
  (parser/parse input))

(deftest test-basic-types
  (testing "Базовые типы C51"
    (let [result (parse-str "int x;")]
      (is (= :identifier (get-in result [:declarations 0 :name :type])))
      (is (= "x" (get-in result [:declarations 0 :name :value])))))
  
  (testing "Void тип"
    (let [result (parse-str "void main() { }")]
      (is (= "void" (get-in result [:declarations 0 :return-type])))
      (is (= "main" (get-in result [:declarations 0 :name]))))))

(deftest test-function-declarations
  (testing "Простая функция"
    (let [result (parse-str "int main() { return 0; }")]
      (is (= "int" (get-in result [:declarations 0 :return-type])))
      (is (= "main" (get-in result [:declarations 0 :name])))
      (is (= 1 (count (get-in result [:declarations 0 :body :statements]))))))
  
  (testing "Функция с параметрами"
    (let [result (parse-str "int sum(int a, int b) { return 0; }")]
      (is (= 2 (count (get-in result [:declarations 0 :params]))))
      (is (= "a" (get-in result [:declarations 0 :params 0 :name])))
      (is (= "b" (get-in result [:declarations 0 :params 1 :name]))))))

(deftest test-expressions
  (testing "Арифметические выражения"
    (let [result (parse-str "int x = 1 + 2 * 3;")]
      (is (= :binary-expr (get-in result [:declarations 0 :value :type])))
      (is (= "+" (get-in result [:declarations 0 :value :operator])))
      (is (= 1 (get-in result [:declarations 0 :value :left :value])))
      (is (= :binary-expr (get-in result [:declarations 0 :value :right :type])))
      (is (= "*" (get-in result [:declarations 0 :value :right :operator])))))
  
  (testing "Битовые операции"
    (let [result (parse-str "int x = a & b | c;")]
      (is (= "|" (get-in result [:declarations 0 :value :operator])))
      (is (= "&" (get-in result [:declarations 0 :value :left :operator]))))))

(deftest test-c51-specific
  (testing "SFR declarations"
    (let [result (parse-str "sfr P0 = 0x80;")]
      (is (= :sfr-decl (get-in result [:declarations 0 :type])))
      (is (= "P0" (get-in result [:declarations 0 :name])))
      (is (= 0x80 (get-in result [:declarations 0 :address :value])))))
  
  (testing "SBIT declarations"
    (let [result (parse-str "sbit LED = P1^5;")]
      (is (= :sbit-decl (get-in result [:declarations 0 :type])))
      (is (= "LED" (get-in result [:declarations 0 :name])))
      (is (= "P1" (get-in result [:declarations 0 :sfr])))
      (is (= 5 (get-in result [:declarations 0 :bit :value])))))
  
  (testing "Interrupt declarations"
    (let [result (parse-str "interrupt 1 void timer0() { }")]
      (is (= :interrupt-decl (get-in result [:declarations 0 :type])))
      (is (= 1 (get-in result [:declarations 0 :number :value])))
      (is (= "timer0" (get-in result [:declarations 0 :function :name]))))))

(deftest test-complex-types
  (testing "Указатели"
    (let [result (parse-str "int *ptr;")]
      (is (= :pointer-type (get-in result [:declarations 0 :type :type])))
      (is (= "int" (get-in result [:declarations 0 :type :base-type])))
      (is (= "ptr" (get-in result [:declarations 0 :name])))))
  
  (testing "Массивы"
    (let [result (parse-str "int arr[10][20];")]
      (is (= :array-type (get-in result [:declarations 0 :type :type])))
      (is (= "int" (get-in result [:declarations 0 :type :base-type])))
      (is (= [10 20] (get-in result [:declarations 0 :type :dimensions])))))
  
  (testing "Структуры"
    (let [result (parse-str "struct Point { int x; int y; };")]
      (is (= :struct-type (get-in result [:declarations 0 :type])))
      (is (= "Point" (get-in result [:declarations 0 :name])))
      (is (= 2 (count (get-in result [:declarations 0 :fields])))))))

(deftest test-error-handling
  (testing "Неправильный синтаксис"
    (is (thrown? Exception (parse-str "int ;")))
    (is (thrown? Exception (parse-str "sfr ;")))
    (is (thrown? Exception (parse-str "sbit LED = ;"))))
  
  (testing "Незакрытые скобки"
    (is (thrown? Exception (parse-str "int main( { return 0; }")))
    (is (thrown? Exception (parse-str "int main() { return 0;"))))
  
  (testing "Отсутствующие точки с запятой"
    (is (thrown? Exception (parse-str "int x")))
    (is (thrown? Exception (parse-str "sfr P0 = 0x80")))))

(deftest test-full-program
  (testing "Полная программа C51"
    (let [program "
      sfr P0 = 0x80;
      sfr P1 = 0x90;
      sbit LED = P1^5;
      
      interrupt 1 void timer0() {
        P0 = 0xFF;
        LED = !LED;
      }
      
      void main() {
        int i;
        int arr[10];
        
        for(i = 0; i < 10; i++) {
          arr[i] = i * 2;
        }
        
        while(1) {
          P0 = 0x00;
          LED = !LED;
        }
      }"
          result (parse-str program)]
      
      (testing "Количество деклараций"
        (is (= 4 (count (:declarations result)))))
      
      (testing "SFR и SBIT декларации"
        (is (= "P0" (get-in result [:declarations 0 :name])))
        (is (= "P1" (get-in result [:declarations 1 :name])))
        (is (= "LED" (get-in result [:declarations 2 :name]))))
      
      (testing "Прерывание timer0"
        (is (= "timer0" (get-in result [:declarations 3 :function :name])))
        (is (= 1 (get-in result [:declarations 3 :number :value]))))
      
      (testing "Главная функция"
        (is (= "main" (get-in result [:declarations 4 :name])))
        (is (= "void" (get-in result [:declarations 4 :return-type])))))))
