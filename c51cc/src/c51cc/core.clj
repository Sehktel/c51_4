(ns c51cc.core
  (:gen-class)
  (:require [c51cc.preprocessor :as preprocessor]
            [c51cc.lexer :as lexer]
            [c51cc.parser :as parser]
            [clojure.java.io :as io]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

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
  ([file-path]
   (try
     (when file-path
       (when (.exists (io/file file-path))
         (let [file-content (slurp file-path)
               _ (println "Прочитан файл:" file-path)
               _ (println "Содержимое файла:" file-content)
               ;; Удаляем комментарии через препроцессор
               clean-content (-> file-content
                               preprocessor/remove-comments
                               preprocessor/remove-whitespace)
               _ (println "Очищенное содержимое:" clean-content)
               ;; Получаем токены через лексер
               tokens (lexer/tokenize clean-content)
               _ (println "Полученные токены:" tokens)]
           tokens)))
     (catch Exception e
       (println "Ошибка при обработке файла:" (.getMessage e))
       nil))))
