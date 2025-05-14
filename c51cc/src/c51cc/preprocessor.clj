(ns c51cc.preprocessor
  (:require [clojure.string :as str]))

(def preprocessor-rules {
    :include "#include"
    :define "#define"
    :undef "#undef"
    :if "#if"
    :ifdef "#ifdef"
    :ifndef "#ifndef"
    :else "#else"
    :elif "#elif"
    :endif "#endif"
    :pragma "#pragma"
    :error "#error"
    :warning "#warning"
    :info "#info"
    :debug "#debug"
    :trace "#trace"
    :line "#line"
    :file "#file"
    :defined "#defined"
})

(defn remove-comments 
  "Удаляет комментарии из кода.
   Поддерживает:
   - Однострочные комментарии (//)
   - Многострочные комментарии (/* */)
   
   Возвращает код без комментариев"
  [code]
  (-> code
      (str/replace #"//.*?(?:\r\n|\r|\n|$)" "\n")  ;; Удаляем однострочные комментарии, сохраняя перенос строки
      (str/replace #"/\*(?s).*?\*/" ""))) ;; Удаляем многострочные комментарии с поддержкой переносов


(defn remove-whitespace [code]
    (str/replace code #"\s+" " ")) ;;?



(def preprocessor-directives [:include :define :undef :if :ifdef :ifndef :else :elif :endif :pragma :error :warning :info :debug :trace :line :file :defined])


