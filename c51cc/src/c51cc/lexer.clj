(ns c51cc.lexer)
(require '[clojure.string :as str]
         '[c51cc.logger :as log])

(declare tokenize-expression
         tokenize)

(def void-keyword {:type :keyword :value "void"})
(def int-keyword {:type :keyword :value "int"})
(def char-keyword {:type :keyword :value "char"})
(def signed-keyword {:type :keyword :value "signed"})
(def unsigned-keyword {:type :keyword :value "unsigned"})
(def if-keyword {:type :keyword :value "if"})
(def else-keyword {:type :keyword :value "else"})
(def for-keyword {:type :keyword :value "for"})
(def while-keyword {:type :keyword :value "while"})
(def return-keyword {:type :keyword :value "return"})
(def main-keyword {:type :keyword :value "main"})
(def interrupt-keyword {:type :keyword :value "interrupt"})
(def sfr-keyword {:type :keyword :value "sfr"})
(def sbit-keyword {:type :keyword :value "sbit"})
(def bit-keyword {:type :keyword :value "bit"})
(def open-round-bracket {:type :bracket :value "("})
(def close-round-bracket {:type :bracket :value ")"})
(def open-curly-bracket {:type :bracket :value "{"})
(def close-curly-bracket {:type :bracket :value "}"})
(def open-square-bracket {:type :bracket :value "["})
(def close-square-bracket {:type :bracket :value "]"})
(def equal {:type :operator :value "="})
(def greater {:type :operator :value ">"})
(def less {:type :operator :value "<"})
(def greater-equal {:type :operator :value ">="})
(def less-equal {:type :operator :value "<="})
(def not-equal {:type :operator :value "!="})
(def and-equal {:type :operator :value "&="})
(def or-equal {:type :operator :value "|="})
(def and-bitwise {:type :operator :value "&&"})
(def or-bitwise {:type :operator :value "||"})
(def semicolon {:type :operator :value ";"})
(def comma {:type :operator :value ","})
(def dot {:type :operator :value "."})
(def colon {:type :operator :value ":"})
;;(def hash {:type :operator :value "#"})
(def plus {:type :operator :value "+"})
(def minus {:type :operator :value "-"})
(def asterisk {:type :operator :value "*"})
(def slash {:type :operator :value "/"})
(def percent {:type :operator :value "%"})
(def tilde {:type :operator :value "~"})
(def pipe {:type :operator :value "|"})
(def ampersand {:type :operator :value "&"})
(def caret {:type :operator :value "^"})
(def exclamation {:type :operator :value "!"})

(def int-number {:type :int-number :value 0})
(def hex-number {:type :hex-number :value 0})
(def identifier {:type :identifier :value ""})

;; Улучшенная функция токенизации для полных выражений
(defn tokenize [input]
  (let [tokens (re-seq #"\w+|[(){}\[\];=<>&,]+|0x[0-9A-Fa-f]+|\d+" input)]
    (if (= (count tokens) 1)
      (cond
        ;; Ключевые слова
        (= input "int") [int-keyword]
        (= input "char") [char-keyword]
        (= input "signed") [signed-keyword]
        (= input "unsigned") [unsigned-keyword]
        (= input "void") [void-keyword]
        (= input "if") [if-keyword]
        (= input "else") [else-keyword]
        (= input "for") [for-keyword]
        (= input "while") [while-keyword]
        (= input "return") [return-keyword]
        (= input "main") [main-keyword]
        (= input "interrupt") [interrupt-keyword]
        (= input "sfr") [sfr-keyword]
        (= input "sbit") [sbit-keyword]
        (= input "bit") [bit-keyword]
        
        ;; Скобки как keywords
        (= input "(") [open-round-bracket]
        (= input ")") [close-round-bracket]
        (= input "{") [open-curly-bracket]
        (= input "}") [close-curly-bracket]
        (= input "[") [open-square-bracket]
        (= input "]") [close-square-bracket]
        
        ;; Операторы
        (= input "=") [equal]
        (= input ">") [greater]
        (= input "<") [less]
        (= input ">=") [greater-equal]
        (= input "&&") [and-bitwise]
        (= input "||") [or-bitwise]
        (= input "&=") [and-equal]
        (= input "|=") [or-equal]
        (= input ";") [semicolon]
        (= input ",") [comma]
        
        ;; Числа как карты
        (re-matches #"^\d+$" input) 
        [{:type :int-number :value (Integer/parseInt input)}]
        
        (re-matches #"^0x[0-9A-Fa-f]+$" input)
        [{:type :hex-number :value (Integer/parseInt (subs input 2) 16)}]
        
        ;; Идентификаторы как карты
        (re-matches #"^[a-zA-Z_][a-zA-Z0-9_]*$" input)
        [{:type :identifier :value input}]
        
        :else nil)
      
      ;; Для полного выражения
      (tokenize-expression input))))

;; Функция для токенизации полного выражения
(defn tokenize-expression [input]
  (let [tokens (re-seq #"\w+|[(){}\[\];=<>&,]+|0x[0-9A-Fa-f]+|\d+" input)]
    (vec (remove nil? (mapv tokenize tokens)))))

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

