(ns c51cc.lexer
  (:require [c51cc.logger :as log]))

(declare tokenize)

;; :TODO add bit type

;; Типы переменных
(def void-keyword     {:type :void-type-keyword     :value "void"     })
(def int-keyword      {:type :int-type-keyword      :value "int"      })
(def char-keyword     {:type :char-type-keyword     :value "char"     })
(def signed-keyword   {:type :signed-type-keyword   :value "signed"   })
(def unsigned-keyword {:type :unsigned-type-keyword :value "unsigned" })

;; Управляющие конструкции
(def if-keyword       {:type :if-control-keyword     :value "if"     })
(def else-keyword     {:type :else-control-keyword   :value "else"   })
(def while-keyword    {:type :while-control-keyword  :value "while"  })
(def for-keyword      {:type :for-control-keyword    :value "for"    })
(def return-keyword   {:type :return-control-keyword :value "return" })

;; Ключевое слово main
(def main-keyword     {:type :main-keyword :value "main" })

;; Специальные ключевые слова микроконтроллера
(def interrupt-keyword  {:type :interrupt-c51-keyword :value "interrupt"  })
(def sfr-keyword        {:type :sfr-c51-keyword       :value "sfr"        })
(def sbit-keyword       {:type :sbit-c51-keyword      :value "sbit"       })
(def using-keyword      {:type :using-c51-keyword     :value "using"      })

;; Скобки
(def open-round-bracket     {:type :open-round-bracket    :value "(" })
(def close-round-bracket    {:type :close-round-bracket   :value ")" })
(def open-curly-bracket     {:type :open-curly-bracket    :value "{" })
(def close-curly-bracket    {:type :close-curly-bracket   :value "}" })
(def open-square-bracket    {:type :open-square-bracket   :value "[" })
(def close-square-bracket   {:type :close-square-bracket  :value "]" })

;; Операторы сравнения
(def greater            {:type :greater-comparison-operator       :value ">"  })
(def less               {:type :less-comparison-operator          :value "<"  })
(def greater-equal      {:type :greater-equal-comparison-operator :value ">=" })
(def less-equal         {:type :less-equal-comparison-operator    :value "<=" })
(def not-equal          {:type :not-equal-comparison-operator     :value "!=" })

;; Операторы присваивания
(def equal      {:type :equal-assignment-operator       :value "="  })
(def and-equal  {:type :and-equal-assignment-operator   :value "&=" })
(def or-equal   {:type :or-equal-assignment-operator    :value "|=" })
(def xor-equal  {:type :xor-equal-assignment-operator   :value "^=" })

;; Битовые операторы
(def and-bitwise {:type :and-bitwise-operator :value "&" })
(def or-bitwise  {:type :or-bitwise-operator  :value "|" })
(def xor-bitwise {:type :xor-bitwise-operator :value "^" })
(def not-bitwise {:type :not-bitwise-operator :value "~" })

;; Разделители
(def semicolon  {:type :semicolon-separator :value ";" })
(def comma      {:type :comma-separator     :value "," })
(def dot        {:type :dot-separator       :value "." })
(def colon      {:type :colon-separator     :value ":" })

;; Арифметические операторы
(def plus       {:type :plus-math-operator      :value "+" })
(def minus      {:type :minus-math-operator     :value "-" })
(def multiply   {:type :multiply-math-operator  :value "*" })
(def divide     {:type :divide-math-operator    :value "/" })
(def modulo     {:type :modulo-math-operator    :value "%" })

;; Логические операторы
(def or-logical         {:type :or-logical-operator         :value "||" })
(def and-logical        {:type :and-logical-operator        :value "&&" })
(def equal-logical      {:type :equal-logical-operator      :value "==" })
(def not-equal-logical  {:type :not-equal-logical-operator  :value "!=" })
(def not-logical        {:type :not-logical-operator        :value "!"  })

;; Числа
(def int-number {:type :int-number :value 0    })
(def hex-number {:type :hex-number :value 0x00 })

;; Идентификаторы
(def identifier {:type :identifier :value "" })

;; Инкремент и декремент
(def increment {:type :increment-operator :value "++" })
(def decrement {:type :decrement-operator :value "--" })

;; Улучшенная функция токенизации для полных выражений
(defn tokenize [input]
  ;; Определяем регулярное выражение для разбиения на токены
  
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
          (= token "void")      [void-keyword]
          (= token "int")       [int-keyword]
          (= token "char")      [char-keyword]
          (= token "signed")    [signed-keyword]
          (= token "unsigned")  [unsigned-keyword]

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
          [{:type :int-number :value (Integer/parseInt token)}]
          
          (re-matches #"^0x[0-9A-Fa-f]+$" token)
          [{:type :hex-number :value (Integer/parseInt (subs token 2) 16)}]
          
          ;; Идентификаторы
          (re-matches #"^[a-zA-Z_][a-zA-Z0-9_]*$" token)
          [{:type :identifier :value token}]
          
          :else nil))
      
      (vec (remove nil? (mapcat tokenize tokens))))))