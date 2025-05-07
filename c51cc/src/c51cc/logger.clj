(ns c51cc.logger
  (:require [clojure.string :as str]))

(declare log-error
         log-warning
         log-info 
         log-debug 
         log-trace
         set-log-level
         can-log
         debug
         log
         current-log-level)

(defonce logger (atom (fn [msg] (println (str "[" (name @current-log-level) "] " msg)))))

;;(defonce logger (atom (fn [msg] (println msg))))

(defonce current-log-level (atom :info))

(def log-levels {
    :silent 0,
    :error 1,
    :warning 2,
    :info 3,
    :debug 4,
    :trace 5
})

(def log-level-names [:silent :error :warning :info :debug :trace])

(defn can-log [level]
  (let [current-level-value (get log-levels @current-log-level 0)
        level-value (get log-levels level 0)]
    (>= current-level-value level-value)))

(defn set-log-level [level]
  (reset! current-log-level level))

(defn log [& msgs]
  (when (can-log @current-log-level)
    (@logger (str/join " " (map str msgs)))))

(defn debug [& msgs]
  (when (can-log :debug)
    (@logger (str/join " " (map str msgs)))))

(defn error [& msgs]
  (when (can-log :error)
    (@logger (str/join " " (map str msgs)))))

(defn warning [& msgs]
  (when (can-log :warning)
    (@logger (str/join " " (map str msgs)))))

(defn info [& msg]
  (when (can-log :info)
    (@logger (str/join " " (map str msg)))))

(defn debug [& msgs]
  (when (can-log :debug)
    (@logger (str/join " " (map str msgs)))))

(defn trace [& msgs]
  (when (can-log :trace)
    (@logger (str/join " " (map str msgs)))))

