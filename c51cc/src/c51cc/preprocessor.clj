(ns c51cc.preprocessor)
(require '[clojure.string :as str]
         '[c51cc.logger :as log]
         '[c51cc.parser :as parser]
         '[c51cc.lexer :as lexer])

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

(defn remove-comments [code]
    (str/replace code #"//.*" "")
    (str/replace code #"/\*.*\*/" ""))


(defn remove-whitespace [code]
    (str/replace code #"\s+" " ")) ;;?



(def preprocessor-directives [:include :define :undef :if :ifdef :ifndef :else :elif :endif :pragma :error :warning :info :debug :trace :line :file :defined])


