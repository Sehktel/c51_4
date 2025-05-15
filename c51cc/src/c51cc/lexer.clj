(ns c51cc.lexer
  (:require [c51cc.logger :as log]))

(declare tokenize)

;; Типы переменных
(def type-keyword {:type :type-keyword
                  :base-type nil     ;; :void | :int | :char
                  :signedness nil    ;; :signed | :unsigned | nil
                  :value nil})       ;; будет содержать оригинальное значение для отладки

;; Предопределенные типы
(def void-type  (assoc type-keyword :base-type :void :value :void))
(def int-type   (assoc type-keyword :base-type :int  :value :int))
(def char-type  (assoc type-keyword :base-type :char :value :char))

;; Управляющие конструкции
(def if-keyword       {:type :control-keyword :value :if     })
(def else-keyword     {:type :control-keyword :value :else   })
(def while-keyword    {:type :control-keyword :value :while  })
(def for-keyword      {:type :control-keyword :value :for    })
(def return-keyword   {:type :control-keyword :value :return })

;; Ключевое слово main
(def main-keyword     {:type :main-keyword :value :main })

;; Специальные ключевые слова микроконтроллера
(def interrupt-keyword  {:type :c51-keyword :value :interrupt  })
(def sfr-keyword        {:type :c51-keyword :value :sfr        })
(def sbit-keyword       {:type :c51-keyword :value :sbit       })
(def using-keyword      {:type :c51-keyword :value :using      })

;; Скобки
(def open-round-bracket     {:type :bracket  :value :open-round   })
(def close-round-bracket    {:type :bracket  :value :close-round  })
(def open-curly-bracket     {:type :bracket  :value :open-curly   })
(def close-curly-bracket    {:type :bracket  :value :close-curly  })
(def open-square-bracket    {:type :bracket  :value :open-square  })
(def close-square-bracket   {:type :bracket  :value :close-square })

;; Операторы сравнения
(def greater            {:type :comparison-operator  :value :greater       })
(def less               {:type :comparison-operator  :value :less          })
(def greater-equal      {:type :comparison-operator  :value :greater-equal })
(def less-equal         {:type :comparison-operator  :value :less-equal    })
(def not-equal          {:type :comparison-operator  :value :not-equal     })

;; Операторы присваивания
(def equal      {:type :assignment-operator   :value :equal     })
(def and-equal  {:type :assignment-operator   :value :and-equal })
(def or-equal   {:type :assignment-operator   :value :or-equal  })
(def xor-equal  {:type :assignment-operator   :value :xor-equal })

;; Битовые операторы
(def and-bitwise {:type :bitwise-operator  :value :and })
(def or-bitwise  {:type :bitwise-operator  :value :or  })
(def xor-bitwise {:type :bitwise-operator  :value :xor })
(def not-bitwise {:type :bitwise-operator  :value :not })

;; Разделители
(def semicolon  {:type :separator   :value  :semicolon })
(def comma      {:type :separator   :value  :comma      })
(def dot        {:type :separator   :value  :dot        })
(def colon      {:type :separator   :value  :colon      })

;; Арифметические операторы
(def plus       {:type :math-operator    :value :plus      })
(def minus      {:type :math-operator    :value :minus     })
(def multiply   {:type :math-operator    :value :multiply  })
(def divide     {:type :math-operator    :value :divide    })
(def modulo     {:type :math-operator    :value :modulo    })
;; Инкремент и декремент
(def increment  {:type :math-operator    :value :increment })
(def decrement  {:type :math-operator    :value :decrement })

;; Логические операторы
(def or-logical         {:type :logical-operator  :value :or        })
(def and-logical        {:type :logical-operator  :value :and       })
(def equal-logical      {:type :logical-operator  :value :equal     })
(def not-equal-logical  {:type :logical-operator  :value :not-equal })
(def not-logical        {:type :logical-operator  :value :not       })

;; Числа
(def number {:type :number 
                 :format :decimal  ;; :decimal | :hex
                 :value 0})

;; Идентификаторы
(def identifier {:type :identifier :value "" })

;; Функция для объединения модификаторов типа
(defn merge-type-modifiers
  "Объединяет базовый тип с модификатором.
   Возвращает nil если комбинация некорректна."
  [base modifier]
  (cond
    ;; Если у нас базовый тип
    (and (:base-type base) (:signedness modifier))
    (assoc base :signedness (:signedness modifier))
    
    ;; Если у нас модификатор
    (and (:signedness base) (:base-type modifier))
    (assoc modifier :signedness (:signedness base))
    
    ;; В остальных случаях возвращаем nil, что означает некорректную комбинацию
    :else nil))

;; Улучшенная функция токенизации для полных выражений
(defn tokenize
"Функция токенезации входной строки [input]
На входе -- строка 
Ны выходе -- набор токенов"
  [input]  
  (let [token-pattern #"\+\+|--|\w+|>=|<=|==|!=|&&|\|\||&=|\|=|\^=|!|[(){}\[\];:=<>&|^~+\-*/%,.]|0x[0-9A-Fa-f]+|\d+"

  ;;( let [token-pattern #">=|<=|==|!=|&&|\|\||&=|\|=|\^=|!|\w+|[(){}\[\];:=<>&|^~+\-*/%,.]|0x[0-9A-Fa-f]+|\d+"
    ;;     #"
    ;;     >=|        ;; Оператор 'больше или равно'
    ;;     <=|        ;; Оператор 'меньше или равно'
    ;;     ==|        ;; Оператор 'равенство'
    ;;     !=|        ;; Оператор 'не равно'
    ;;     &&|        ;; Логическое И
    ;;     \|\||      ;; Логическое ИЛИ (экранировано из-за спец-символа)
    ;;     &=|        ;; Побитовое И с присваиванием
    ;;     \|=|       ;; Побитовое ИЛИ с присваиванием
    ;;     \^=|       ;; Побитовое XOR с присваиванием
    ;;     !|         ;; Логическое отрицание
    ;;     \w+|       ;; Идентификаторы (буквы, цифры, подчеркивание)
    ;;     [(){}\[\];:=<>&|^~+\-*/%,.]|  ;; Скобки, операторы, разделители
    ;;     0x[0-9A-Fa-f]+|  ;; Шестнадцатеричные числа
    ;;     \d+        ;; Десятичные числа
    ;;     " 
    tokens (re-seq token-pattern input)]
    (if (= (count tokens) 1)
      (let [token (first tokens)]
        (cond
          ;; Ключевые слова
          (= token "void")      [void-type]
          (= token "int")       [int-type]
          (= token "char")      [char-type]
          (= token "signed")    {:type :type-keyword :base-type nil :signedness :signed :value nil}
          (= token "unsigned")  {:type :type-keyword :base-type nil :signedness :unsigned :value nil}

          ;; Управляющие конструкции
          (= token "if")     [if-keyword]
          (= token "else")   [else-keyword]
          (= token "for")    [for-keyword]
          (= token "while")  [while-keyword]
          (= token "return") [return-keyword]

          ;; Ключевое слово main
          (= token "main") [main-keyword]

          ;; Специальные ключевые слова микроконтроллера
          (= token "interrupt") [interrupt-keyword]
          (= token "sfr")       [sfr-keyword]
          (= token "sbit")      [sbit-keyword]
          (= token "using")     [using-keyword]

          ;; Скобки
          (= token "(") [open-round-bracket]
          (= token ")") [close-round-bracket]
          (= token "{") [open-curly-bracket]
          (= token "}") [close-curly-bracket]
          (= token "[") [open-square-bracket]
          (= token "]") [close-square-bracket]

          ;; Операторы сравнения
          (= token ">")  [greater]
          (= token "<")  [less]
          (= token ">=") [greater-equal]
          (= token "<=") [less-equal]
          (= token "!=") [not-equal]

          ;; Операторы присваивания
          (= token "&=") [and-equal]
          (= token "|=") [or-equal]
          (= token "^=") [xor-equal]
          (= token "=")  [equal]

          ;; Логические операторы
          (= token "||") [or-logical]
          (= token "&&") [and-logical]
          (= token "!")  [not-logical]
          (= token "==") [equal-logical]

          ;; Битовые операторы
          (= token "&") [and-bitwise]
          (= token "|") [or-bitwise]
          (= token "^") [xor-bitwise]
          (= token "~") [not-bitwise]

          ;; Разделители
          (= token ";") [semicolon]
          (= token ",") [comma]
          (= token ".") [dot]
          (= token ":") [colon]

          ;; Инкремент и декремент
          (= token "++") [increment]
          (= token "--") [decrement]
          
          ;; Арифметические операторы
          (= token "+") [plus]
          (= token "-") [minus]
          (= token "*") [multiply]
          (= token "/") [divide]
          (= token "%") [modulo]
          
          ;; Числа
          (re-matches #"^\d+$" token) 
          [{:type :number :format :decimal :value (Integer/parseInt token)}]
          
          (re-matches #"^0x[0-9A-Fa-f]+$" token)
          [{:type :number :format :hex     :value (Integer/parseInt (subs token 2) 16)}]
          
          ;; Идентификаторы
          (re-matches #"^[a-zA-Z_][a-zA-Z0-9_]*$" token)
          [{:type :identifier :value token}]
          
          :else nil))
      
      (vec (remove nil? (mapcat tokenize tokens))))))