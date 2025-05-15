(ns parser.core
  (:require [clojure.spec.alpha :as s]))

;; Спецификация состояния парсера
(s/def ::tokens vector?)
(s/def ::position nat-int?)
(s/def ::ast map?)
(s/def ::errors vector?)
(s/def ::state #{:initial :parsing-declaration :parsing-function 
                 :parsing-statement :parsing-expression})
(s/def ::parser-state (s/keys :req-un [::tokens ::position ::ast 
                                      ::errors ::state]))

;; Начальное состояние парсера
(defn create-initial-state [tokens]
  {:tokens tokens
   :position 0
   :ast {:declarations [] 
         :functions []}
   :errors []
   :state :initial})

;; Основные функции парсера

(defn parse-declaration 
  "Парсинг объявлений переменных и типов"
  [{:keys [tokens position] :as state}]
  ;; TODO: Имплементация парсинга объявлений
  state)

(defn parse-function 
  "Парсинг определения функции"
  [{:keys [tokens position] :as state}]
  ;; TODO: Имплементация парсинга функций
  state)

(defn parse-statement 
  "Парсинг отдельного выражения"
  [{:keys [tokens position] :as state}]
  ;; TODO: Имплементация парсинга выражений
  state)

(defn parse-expression 
  "Парсинг выражений"
  [{:keys [tokens position] :as state}]
  ;; TODO: Имплементация парсинга выражений
  state)

;; FSM transitions
(def transitions
  {:initial {:declaration :parsing-declaration
            :function :parsing-function}
   :parsing-declaration {:end-declaration :initial
                        :error :initial}
   :parsing-function {:statement :parsing-statement
                     :end-function :initial
                     :error :initial}
   :parsing-statement {:expression :parsing-expression
                      :end-statement :parsing-function
                      :error :parsing-function}
   :parsing-expression {:end-expression :parsing-statement
                       :error :parsing-statement}})

(defn transition 
  "Функция перехода состояний FSM"
  [current-state event]
  (get-in transitions [current-state event]))

;; Главная функция парсера
(defn parse [tokens]
  (loop [state (create-initial-state tokens)]
    (if (>= (:position state) (count (:tokens state)))
      state
      (let [current-token (nth (:tokens state) (:position state))
            next-state (case (:state state)
                        :initial (cond
                                  (declaration? current-token) (parse-declaration state)
                                  (function? current-token) (parse-function state)
                                  :else (update state :errors conj "Unexpected token"))
                        :parsing-declaration (parse-declaration state)
                        :parsing-function (parse-function state)
                        :parsing-statement (parse-statement state)
                        :parsing-expression (parse-expression state))]
        (recur next-state))))) 