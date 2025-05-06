(ns c51cc.lexer)
(require '[clojure.string :as str]
         '[c51cc.logger :as log])

(declare tokenize-expression
         tokenize)

;; Типы переменных
(def void-keyword {:type :type-keyword :value "void"})
(def int-keyword {:type :type-keyword :value "int"})
(def char-keyword {:type :type-keyword :value "char"})
(def signed-keyword {:type :type-keyword :value "signed"})
(def unsigned-keyword {:type :type-keyword :value "unsigned"})

;; Управляющие конструкции
(def if-keyword {:type :control-keyword :value "if"})
(def else-keyword {:type :control-keyword :value "else"})
(def for-keyword {:type :control-keyword :value "for"})
(def while-keyword {:type :control-keyword :value "while"})
(def return-keyword {:type :control-keyword :value "return"})

;; Ключевое слово main
(def main-keyword {:type :main-keyword :value "main"})

;; Специальные ключевые слова микроконтроллера
(def interrupt-keyword {:type :c51-keyword :value "interrupt"})
(def sfr-keyword {:type :c51-keyword :value "sfr"})
(def sbit-keyword {:type :c51-keyword :value "sbit"})
(def using-keyword {:type :c51-keyword :value "using"})

;; Скобки
(def open-round-bracket {:type :bracket :value "("})
(def close-round-bracket {:type :bracket :value ")"})
(def open-curly-bracket {:type :bracket :value "{"})
(def close-curly-bracket {:type :bracket :value "}"})
(def open-square-bracket {:type :bracket :value "["})
(def close-square-bracket {:type :bracket :value "]"})

;; Операторы сравнения
(def greater {:type :comparison-operator :value ">"})
(def less {:type :comparison-operator :value "<"})
(def greater-equal {:type :comparison-operator :value ">="})
(def less-equal {:type :comparison-operator :value "<="})
(def not-equal {:type :comparison-operator :value "!="})

;; Операторы присваивания
(def equal {:type :assignment-operator :value "="})
(def and-equal {:type :assignment-operator :value "&="})
(def or-equal {:type :assignment-operator :value "|="})
(def xor-equal {:type :assignment-operator :value "^="})

;; Битовые операторы
(def and-bitwise {:type :bitwise-operator :value "&"})
(def or-bitwise {:type :bitwise-operator :value "|"})
(def xor-bitwise {:type :bitwise-operator :value "^"})
(def bitwise-not {:type :bitwise-operator :value "~"})

;; Разделители
(def semicolon {:type :separator :value ";"})
(def comma {:type :separator :value ","})
(def dot {:type :separator :value "."})
(def colon {:type :separator :value ":"})
;;(def hash {:type :separator :value "#"})

;; Арифметические операторы
(def plus {:type :math-operator :value "+"})
(def minus {:type :math-operator :value "-"})
(def multiply {:type :math-operator :value "*"})
(def divide {:type :math-operator :value "/"})
(def modulo {:type :math-operator :value "%"})

;; Логические операторы
(def or-logical {:type :logical-operator :value "||"})
(def and-logical {:type :logical-operator :value "&&"})
(def equal-logical {:type :logical-operator :value "=="})
(def not-equal-logical {:type :logical-operator :value "!="})
(def not-logical {:type :logical-operator :value "!"})

;; Числа
(def int-number {:type :number :value 0})
(def hex-number {:type :number :value 0x00})

;; Идентификаторы
(def identifier {:type :identifier :value ""})

;; Улучшенная функция токенизации для полных выражений
(defn tokenize [input]
  ;; Определяем регулярное выражение для разбиения на токены
  (let [token-pattern #">=|<=|==|!=|&&|\|\||&=|\|=|\^=|!|\w+|[(){}\[\];:=<>&|^~+\-*/%,.]|0x[0-9A-Fa-f]+|\d+"
        tokens (re-seq token-pattern input)]
    (if (= (count tokens) 1)
      (let [token (first tokens)]
        (cond
          ;; Ключевые слова
          (= token "void") [{:type :type-keyword :value "void"}]
          (= token "int") [{:type :type-keyword :value "int"}]
          (= token "char") [{:type :type-keyword :value "char"}]
          (= token "signed") [{:type :type-keyword :value "signed"}]
          (= token "unsigned") [{:type :type-keyword :value "unsigned"}]

          ;; Управляющие конструкции
          (= token "if") [{:type :control-keyword :value "if"}]
          (= token "else") [{:type :control-keyword :value "else"}]
          (= token "for") [{:type :control-keyword :value "for"}]
          (= token "while") [{:type :control-keyword :value "while"}]
          (= token "return") [{:type :control-keyword :value "return"}]

          ;; Ключевое слово main
          (= token "main") [{:type :main-keyword :value "main"}]

          ;; Специальные ключевые слова микроконтроллера
          (= token "interrupt") [{:type :c51-keyword :value "interrupt"}]
          (= token "sfr") [{:type :c51-keyword :value "sfr"}]
          (= token "sbit") [{:type :c51-keyword :value "sbit"}]
          (= token "using") [{:type :c51-keyword :value "using"}]

          ;; Скобки
          (= token "(") [{:type :open-round-bracket :value "("}]
          (= token ")") [{:type :close-round-bracket :value ")"}]
          (= token "{") [{:type :open-curly-bracket :value "{"}]
          (= token "}") [{:type :close-curly-bracket :value "}"}]
          (= token "[") [{:type :open-square-bracket :value "["}]
          (= token "]") [{:type :close-square-bracket :value "]"}]

          ;; Операторы сравнения
          (= token ">") [{:type :comparison-operator :value ">"}]
          (= token "<") [{:type :comparison-operator :value "<"}]
          (= token ">=") [{:type :comparison-operator :value ">="}]
          (= token "<=") [{:type :comparison-operator :value "<="}]
          (= token "!=") [{:type :comparison-operator :value "!="}]

          ;; Операторы присваивания
          (= token "&=") [{:type :assignment-operator :value "&="}]
          (= token "|=") [{:type :assignment-operator :value "|="}]
          (= token "^=") [{:type :assignment-operator :value "^="}]
          (= token "=") [{:type :assignment-operator :value "="}]

          ;; Логические операторы
          (= token "||") [{:type :logical-operator :value "||"}]
          (= token "&&") [{:type :logical-operator :value "&&"}]
          (= token "!") [{:type :logical-operator :value "!"}]
          (= token "==") [{:type :logical-operator :value "=="}]

          ;; Битовые операторы
          (= token "&") [{:type :bitwise-operator :value "&"}]
          (= token "|") [{:type :bitwise-operator :value "|"}]
          (= token "^") [{:type :bitwise-operator :value "^"}]
          (= token "~") [{:type :bitwise-operator :value "~"}]

          ;; Разделители
          (= token ";") [{:type :separator :value ";"}]
          (= token ",") [{:type :separator :value ","}]
          (= token ".") [{:type :separator :value "."}]
          (= token ":") [{:type :separator :value ":"}]
          
          ;; Арифметические операторы
          (= token "+") [{:type :math-operator :value "+"}]
          (= token "-") [{:type :math-operator :value "-"}]
          (= token "*") [{:type :math-operator :value "*"}]
          (= token "/") [{:type :math-operator :value "/"}]
          (= token "%") [{:type :math-operator :value "%"}]
          
          ;; Числа
          (re-matches #"^\d+$" token) 
          [{:type :int-number :value (Integer/parseInt token)}]
          
          (re-matches #"^0x[0-9A-Fa-f]+$" token)
          [{:type :hex-number :value (Integer/parseInt (subs token 2) 16)}]
          
          ;; Идентификаторы
          (re-matches #"^[a-zA-Z_][a-zA-Z0-9_]*$" token)
          [{:type :identifier :value token}]
          
          :else nil))
      
      ;; Для полного выражения
      (vec (remove nil? (mapcat tokenize tokens))))))

;; ;; Функция для токенизации полного выражения
;; (defn tokenize-expression [input]
;; ;;   (let [tokens (re-seq #"\w+|[(){}\[\];=<>&,]+|0x[0-9A-Fa-f]+|\d+" input)]
;; ;;     (vec (remove nil? (apply concat (mapv tokenize tokens))))))
;;   (let [tokens (re-seq #"\s+|\w+|[(){}\[\];=<>&,]+|0x[0-9A-Fa-f]+|\d+" input)]
;;     (vec (remove nil? (apply concat (mapv tokenize (filter #(not (re-matches #"\s+" %)) tokens)))))))

(comment
  (tokenize-expression "int main() { return 0; }")
  ;;📘 Теоретический контекст:
  ;;Данная функция реализует первый этап компиляторной трансформации - лексический анализ (лексинг), который преобразует последовательность символов в последовательность токенов.
  ;;🧠 Декомпозиция функции:
  ;;Регулярное выражение #"\w+|[(){}\[\];=<>&,]+|0x[0-9A-Fa-f]+|\d+" разбивает входную строку на токены:
  ;; \w+: Слова (идентификаторы)
  ;; [(){}\[\];=<>&,]+: Специальные символы и операторы
  ;; 0x[0-9A-Fa-f]+: Шестнадцатеричные числа
  ;; \d+: Десятичные числа
  ;; re-seq генерирует последовательность всех найденных токенов
  ;; mapv tokenize применяет функцию tokenize к каждому токену, превращая сырые строки в семантические представления
)

