(ns c51cc.lexer_test
  (:require [clojure.test :refer :all]
            [c51cc.lexer :as lexer]
            [c51cc.logger :as log]))

;; Тесты для токенизации ключевых слов
(deftest test-keyword-tokenization
  (testing "Токенизация основных типов данных"
    (is (= [{:type :type-keyword
             :base-type :void
             :signedness nil
             :value :void}] (lexer/tokenize "void")))
    
    (is (= [{:type :type-keyword
             :base-type :int
             :signedness nil
             :value :int}] (lexer/tokenize "int")))
    
    (is (= [{:type :type-keyword
             :base-type :char
             :signedness nil
             :value :char}] (lexer/tokenize "char")))
    
    (is (= {:type :type-keyword
            :base-type nil
            :signedness :signed
            :value nil} (lexer/tokenize "signed")))
    
    (is (= {:type :type-keyword
            :base-type nil
            :signedness :unsigned
            :value nil} (lexer/tokenize "unsigned"))))

  (testing "Токенизация составных типов"
    (is (= {:type :type-keyword
            :base-type :int
            :signedness :signed
            :value :int}
           (lexer/merge-type-modifiers 
             {:type :type-keyword :base-type :int :signedness nil :value :int}
             {:type :type-keyword :base-type nil :signedness :signed :value nil})))
    
    (is (= {:type :type-keyword
            :base-type :char
            :signedness :unsigned
            :value :char}
           (lexer/merge-type-modifiers
             {:type :type-keyword :base-type :char :signedness nil :value :char}
             {:type :type-keyword :base-type nil :signedness :unsigned :value nil}))))

  (testing "Токенизация управляющих конструкций"
    (is (= [{:type :control-keyword :value :if    }] (lexer/tokenize "if"    )))
    (is (= [{:type :control-keyword :value :else  }] (lexer/tokenize "else"  )))
    (is (= [{:type :control-keyword :value :for   }] (lexer/tokenize "for"   )))
    (is (= [{:type :control-keyword :value :while }] (lexer/tokenize "while" )))
    (is (= [{:type :control-keyword :value :return}] (lexer/tokenize "return")))))

;;Тест для ключевого слова main
(deftest test-main-keyword
  (testing "Токенизация ключевого слова main"
    (is (= [{:type :main-keyword, :value :main}] (lexer/tokenize "main")))))

;; Тесты для Специальные ключевые слова микроконтроллера
(deftest test-c51-keywords
  (testing "Токенизация специальных ключевых слов микроконтроллера"
    (is (= [{:type :c51-keyword :value :interrupt}] (lexer/tokenize "interrupt")))
    (is (= [{:type :c51-keyword :value :sfr      }] (lexer/tokenize "sfr"      )))
    (is (= [{:type :c51-keyword :value :sbit     }] (lexer/tokenize "sbit"     )))
    (is (= [{:type :c51-keyword :value :using    }] (lexer/tokenize "using"    )))))

;; Тесты для токенизации скобок
(deftest test-bracket-tokenization
  (testing "Токенизация различных типов скобок"
    (is (= [{:type :bracket :value :open-round  }] (lexer/tokenize "(")))
    (is (= [{:type :bracket :value :close-round }] (lexer/tokenize ")")))
    (is (= [{:type :bracket :value :open-curly  }] (lexer/tokenize "{")))
    (is (= [{:type :bracket :value :close-curly }] (lexer/tokenize "}")))
    (is (= [{:type :bracket :value :open-square }] (lexer/tokenize "[")))
    (is (= [{:type :bracket :value :close-square}] (lexer/tokenize "]")))
    ;; (is (= [{:type :bracket, :value :open-angle  }] (lexer/tokenize "<")))
    ;; (is (= [{:type :bracket, :value :close-angle }] (lexer/tokenize ">")))
    ))

;; Операторы сравнения
(deftest test-comparison-operator-tokenization
  (testing "Токенизация операторов сравнения"
    (is (= [{:type :comparison-operator :value :greater      }] (lexer/tokenize ">" )))
    (is (= [{:type :comparison-operator :value :less         }] (lexer/tokenize "<" )))
    (is (= [{:type :comparison-operator :value :greater-equal}] (lexer/tokenize ">=")))
    (is (= [{:type :comparison-operator :value :less-equal   }] (lexer/tokenize "<=")))
    (is (= [{:type :comparison-operator :value :not-equal    }] (lexer/tokenize "!=")))
    ))

;; Операторы присваивания
(deftest test-assignment-operator-tokenization
  (testing "Токенизация операторов присваивания"
    (is (= [{:type :assignment-operator :value :equal     }] (lexer/tokenize "=" )))
    (is (= [{:type :assignment-operator :value :and-equal }] (lexer/tokenize "&=")))
    (is (= [{:type :assignment-operator :value :or-equal  }] (lexer/tokenize "|=")))
    (is (= [{:type :assignment-operator :value :xor-equal }] (lexer/tokenize "^=")))))

;; Битовые операторы
(deftest test-bitwise-operator-tokenization
  (testing "Токенизация битовых операторов"
    (is (= [{:type :bitwise-operator :value :and}] (lexer/tokenize "&")))
    (is (= [{:type :bitwise-operator :value :or }] (lexer/tokenize "|")))
    (is (= [{:type :bitwise-operator :value :xor}] (lexer/tokenize "^")))
    (is (= [{:type :bitwise-operator :value :not}] (lexer/tokenize "~")))))

;; Разделители
(deftest test-separator-tokenization
  (testing "Токенизация разделителей"
    (is (= [{:type :separator :value :semicolon}] (lexer/tokenize ";")))
    (is (= [{:type :separator :value :comma    }] (lexer/tokenize ",")))
    (is (= [{:type :separator :value :dot      }] (lexer/tokenize ".")))
    (is (= [{:type :separator :value :colon    }] (lexer/tokenize ":")))))

;; Тесты для токенизации чисел
(deftest test-number-tokenization
  (testing "Токенизация целых чисел"
    (is (= [{:type :number :format :decimal :value 42 }] (lexer/tokenize "42")))
    (is (= [{:type :number :format :decimal :value 0  }] (lexer/tokenize "0"))))
  
  (testing "Токенизация шестнадцатеричных чисел"
    (is (= [{:type :number :value 42  :format :hex}] (lexer/tokenize "0x2A")))
    (is (= [{:type :number :value 255 :format :hex}] (lexer/tokenize "0xFF")))))


;; Арифметические операторы
(deftest test-arithmetic-operator-tokenization
  (testing "Токенизация арифметических операторов"
    (is (= [{:type :math-operator :value :plus    }] (lexer/tokenize "+")))
    (is (= [{:type :math-operator :value :minus   }] (lexer/tokenize "-")))
    (is (= [{:type :math-operator :value :multiply}] (lexer/tokenize "*")))
    (is (= [{:type :math-operator :value :divide  }] (lexer/tokenize "/")))
    (is (= [{:type :math-operator :value :modulo  }] (lexer/tokenize "%"))))) 

;; Логические операторы
(deftest test-logical-operator-tokenization
  (testing "Токенизация логических операторов"
    (is (= [{:type :logical-operator :value :or }] (lexer/tokenize "||")))
    (is (= [{:type :logical-operator :value :and}] (lexer/tokenize "&&")))
    (is (= [{:type :logical-operator :value :not}] (lexer/tokenize "!")))
    ))



;; Тесты для токенизации составных выражений
(deftest test-complex-tokenization
  (testing "Токенизация простой main функции"
    (is (= [{:type :type-keyword
             :base-type :void
             :signedness nil
             :value :void}
            {:type :main-keyword :value :main} 
            {:type :bracket :value :open-round}
            {:type :bracket :value :close-round}
            {:type :bracket :value :open-curly}
            {:type :control-keyword :value :return}
            {:type :number :format :decimal :value 0}
            {:type :separator :value :semicolon}
            {:type :bracket :value :close-curly}]
           (lexer/tokenize "void main() { return 0; }")))))

;; Тесты для специфических ключевых слов микроконтроллера
(deftest test-microcontroller-keywords
  (testing "Токенизация специфических ключевых слов"
    (is (= [{:type :c51-keyword :value :interrupt}] (lexer/tokenize "interrupt")))
    (is (= [{:type :c51-keyword :value :sfr}] (lexer/tokenize "sfr")))
    (is (= [{:type :c51-keyword :value :sbit}] (lexer/tokenize "sbit")))
    (is (= [{:type :c51-keyword :value :using}] (lexer/tokenize "using")))))

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
    (is (= [{:type :type-keyword
             :base-type :int
             :signedness nil
             :value :int} 
            {:type :identifier :value "example_func"} 
            {:type :bracket :value :open-round} 
            {:type :identifier :value "param1"}
            {:type :separator :value :comma} 
            {:type :number :format :decimal :value 42} 
            {:type :bracket :value :close-round} 
            {:type :separator :value :semicolon}]
           (lexer/tokenize "int example_func(param1, 42);")))
    
    (is (= [{:type :control-keyword :value :if} 
            {:type :bracket :value :open-round} 
            {:type :identifier :value "x"} 
            {:type :comparison-operator :value :greater-equal} 
            {:type :number :format :decimal :value 10} 
            {:type :logical-operator :value :and} 
            {:type :identifier :value "y"} 
            {:type :comparison-operator :value :less} 
            {:type :number :format :decimal :value 100} 
            {:type :bracket :value :close-round}]
           (lexer/tokenize "if (x >= 10 && y < 100)")))))

;; Добавим тесты для обработки краевых случаев
(deftest test-edge-cases
  (testing "Обработка сложных идентификаторов и чисел"
    (is (= [{:type :identifier :value "_underscore_var"}] 
           (lexer/tokenize "_underscore_var")))
    
    (is (= [{:type :number :format :hex :value 255}] 
           (lexer/tokenize "0xFF")))
    
    (is (= [{:type :type-keyword
             :base-type :int
             :signedness nil
             :value :int}
            {:type :identifier :value "x"} 
            {:type :assignment-operator :value :equal} 
            {:type :number :format :hex :value 15} 
            {:type :separator :value :semicolon}]
           (lexer/tokenize "int x = 0xF;"))))
  
  (testing "Обработка составных операторов"
    (is (= [{:type :type-keyword
             :base-type :int
             :signedness nil
             :value :int}
            {:type :identifier :value "x"} 
            {:type :assignment-operator :value :and-equal} 
            {:type :number :format :decimal :value 10} 
            {:type :separator :value :semicolon}]
           (lexer/tokenize "int x &= 10;")))))

;; Тесты для инкремента и декремента
(deftest test-increment-decrement-tokenization
  (testing "Токенизация операторов инкремента и декремента"
    (testing "Префиксный инкремент"
      (is (= [{:type :math-operator :value :increment}
              {:type :identifier :value "x"}] 
             (lexer/tokenize "++x"))))
    
    (testing "Постфиксный инкремент"
      (is (= [{:type :identifier :value "x"}
              {:type :math-operator :value :increment}] 
             (lexer/tokenize "x++"))))
    
    (testing "Префиксный декремент"
      (is (= [{:type :math-operator :value :decrement}
              {:type :identifier :value "y"}] 
             (lexer/tokenize "--y"))))
    
    (testing "Постфиксный декремент"
      (is (= [{:type :identifier :value "y"}
              {:type :math-operator :value :decrement}] 
             (lexer/tokenize "y--"))))
    
    (testing "Сложные выражения с инкрементом и декрементом"
      (is (= [{:type :type-keyword
               :base-type :int
               :signedness nil
               :value :int}
              {:type :identifier :value "x"}
              {:type :assignment-operator :value :equal}
              {:type :math-operator :value :increment}
              {:type :identifier :value "y"}
              {:type :separator :value :semicolon}]
             (lexer/tokenize "int x = ++y;")))
      
      (is (= [{:type :type-keyword
               :base-type :int
               :signedness nil
               :value :int}
              {:type :identifier :value "x"}
              {:type :assignment-operator :value :equal}
              {:type :identifier :value "y"}
              {:type :math-operator :value :increment}
              {:type :separator :value :semicolon}]
             (lexer/tokenize "int x = y++;")))
      
      (is (= [{:type :type-keyword
               :base-type :int
               :signedness nil
               :value :int}
              {:type :identifier :value "x"}
              {:type :assignment-operator :value :equal}
              {:type :math-operator :value :decrement}
              {:type :identifier :value "y"}
              {:type :separator :value :semicolon}]
             (lexer/tokenize "int x = --y;")))
      
      (is (= [{:type :type-keyword
               :base-type :int
               :signedness nil
               :value :int}
              {:type :identifier :value "x"}
              {:type :assignment-operator :value :equal}
              {:type :identifier :value "y"}
              {:type :math-operator :value :decrement}
              {:type :separator :value :semicolon}]
             (lexer/tokenize "int x = y--;"))))))