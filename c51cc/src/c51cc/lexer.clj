(ns c51cc.lexer)

;; Улучшенная функция токенизации для полных выражений
(defn tokenize [input]
  (let [tokens (re-seq #"\w+|[(){}\[\];=<>&,]+|0x[0-9A-Fa-f]+|\d+" input)]
    (if (= (count tokens) 1)
      ;; Для одиночного токена - старая логика
      (cond
        ;; Ключевые слова
        (= input "int") [:int]
        (= input "char") [:char]
        (= input "signed") [:signed]
        (= input "unsigned") [:unsigned]
        (= input "void") [:void]
        (= input "if") [:if]
        (= input "else") [:else]
        (= input "for") [:for]
        (= input "while") [:while]
        (= input "return") [:return]
        (= input "main") [:main]
        (= input "interrupt") [:interrupt]
        (= input "sfr") [:sfr]
        (= input "sbit") [:sbit]
        (= input "bit") [:bit]
        
        ;; Скобки
        (= input "(") [:open-round-bracket]
        (= input ")") [:close-round-bracket]
        (= input "{") [:open-curly-bracket]
        (= input "}") [:close-curly-bracket]
        (= input "[") [:open-square-bracket]
        (= input "]") [:close-square-bracket]
        
        ;; Операторы
        (= input "=") [:equal]
        (= input ">") [:greater]
        (= input "<") [:less]
        (= input ">=") [:greater-equal]
        (= input "&&") [:and]
        (= input "&=") [:and-equal]
        (= input ";") [:semicolon]
        (= input ",") [:comma]
        
        ;; Числа
        (re-matches #"^\d+$" input) 
        [{:type :int-number :value (Integer/parseInt input)}]
        
        (re-matches #"^0x[0-9A-Fa-f]+$" input)
        [{:type :hex-number :value (Integer/parseInt (subs input 2) 16)}]
        
        ;; Идентификаторы
        (re-matches #"^[a-zA-Z_][a-zA-Z0-9_]*$" input)
        [{:type :identifier :value input}]
        
        :else nil)
      
      ;; Для полного выражения - используем tokenize-expression
      (first (remove nil? (mapv tokenize tokens))))))

;; Функция для токенизации полного выражения
(defn tokenize-expression [input]
  (let [tokens (re-seq #"\w+|[(){}\[\];=<>&,]+|0x[0-9A-Fa-f]+|\d+" input)]
    (remove nil? (mapv tokenize tokens))))

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

