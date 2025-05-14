(ns c51cc.core
  (:gen-class)
  (:require [c51cc.preprocessor :as preprocessor]
            [c51cc.lexer :as lexer]
            [c51cc.logger :as log]
            [c51cc.parser :as parser]
            [c51cc.ast :as ast]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(declare get-test-file-tokens
         pretty-print-ast )

(defn -main
  "Главная функция приложения, точка входа.

  Обрабатывает аргументы командной строки:
  - Если первый аргумент \"help\", выводит справочную информацию.
  - В противном случае, пытается обработать C51 файл, указанный
    в качестве аргумента или через переменную окружения TEST_FILE.
    Вызывает `get-test-file-tokens` для получения токенов из файла.
    Далее вызывается pretty-print-ast для печати AST дерева."
  [& args]
  (let [first-arg (first args)]
    (if (= "help" first-arg)
      ;; Если первый аргумент "help", выводим справочную информацию
      (do
        (println "c51cc - Is a C51 Clojure Compiler")
        (println "Usage: clojure -M -m c51cc.core [file-path | \"help\"]")
        (println "\nArguments:")
        (println "  file-path    Path to the C51 source file.")
        (println "               If omitted, attempts to use the TEST_FILE environment variable.")
        (println "  \"help\"       Display this help message and exit.")
        (println "\nExample:")
        (println "  clojure -M -m c51cc.core your_file.c")
        (println "  clojure -M -m c51cc.core help"))
      ;; В противном случае, вызываем get-test-file-tokens с первым аргументом
      ;; (который может быть путем к файлу или nil, если аргументы отсутствуют)
      (let [tokens (get-test-file-tokens first-arg)
            ast (parser/parser tokens)
            _ (pretty-print-ast ast)])
      )))

(defn test-function
  "Тестовая функция"
  []
  (log/info "Тестовая функция!"))

(defn get-test-file-tokens
  "Получает токены из файла.
   
   Аргументы:
   file-path (опционально) - путь к файлу напрямую
   Если file-path не указан, пытается получить путь из переменной окружения TEST_FILE
   
   Проходит следующие этапы:
   1. Получение пути к файлу (из аргумента или переменной окружения)
   2. Удаление комментариев через препроцессор
   3. Токенизация через лексер
   4. Подготовка токенов для парсера
   
   Возвращает вектор токенов или nil в случае ошибки"
  ([]
   (get-test-file-tokens (System/getenv "TEST_FILE")))
  ;; TODO: изменить println на log/debug
  ([file-path]
   (try
     (when file-path
       (when (.exists (io/file file-path))
         (let [file-content (slurp file-path)
               _ (log/debug "Прочитан файл:" file-path)
               _ (log/debug "Содержимое файла:" file-content)
               ;; Удаляем комментарии через препроцессор
               clean-content (-> file-content
                               preprocessor/remove-comments
                               preprocessor/remove-whitespace)
               _ (log/debug "Очищенное содержимое:" clean-content)
               ;; Получаем токены через лексер
               tokens (lexer/tokenize clean-content)
               _ (log/debug "Полученные токены:" tokens)]
           tokens)))
     (catch Exception e
       (log/error "Ошибка при обработке файла:" (.getMessage e))
       nil))))

(defn pretty-print-ast [ast]
  (let [result (ast/pretty-print ast)]
    ;; Разделяем результат на строки и печатаем каждую отдельно
    (doseq [line (str/split-lines result)]
      (println line))
    result))