(ns c51cc.logger
  (:require [clojure.string :as str]))


(declare log-error
         log-warning
         log-info 
         log-debug 
         log-trace
         log-paranoic
         set-log-level
         can-log
         debug
         log
         current-log-level
         print-current-log-level)

(defonce logger 
  (atom (fn [level msg] 
    (println (str "[" (name level) "] " msg)))))

(defonce current-log-level (atom :info))

(def log-levels {
    :silent 0,
    :error 1,
    :warning 2,
    :info 3,
    :debug 4,
    :trace 5,
    :paranoic 6
})

(defn print-current-log-level []
  (println (str "Current log level: " @current-log-level)))

(def log-level-names [:silent :error :warning :info :debug :trace :paranoic])

(defn can-log [level]
  (let [current-level-value (get log-levels @current-log-level 0)
        level-value (get log-levels level 0)]
    (>= current-level-value level-value)))

(defn set-log-level [level]
  (reset! current-log-level level))

(defn log [& msgs]
  (when (can-log @current-log-level)
    (@logger @current-log-level (str/join " " (map str msgs)))))

(defn silent [& msgs])
  ;; (when (can-log :silent)
  ;;   (@logger (str/join " " (map str msgs)))))

(defn error [& msgs]
  (when (can-log :error)
    (@logger :error (str/join " " (map str msgs)))))

(defn warning [& msgs]
  (when (can-log :warning)
    (@logger :warning (str/join " " (map str msgs)))))

(defn info [& msg]
  (when (can-log :info)
    (@logger :info (str/join " " (map str msg)))))

(defn debug [& msgs]
  (when (can-log :debug)
    (@logger :debug (str/join " " (map str msgs)))))

(defn trace [& msgs]
  (when (can-log :trace)
    (@logger :trace (str/join " " (map str msgs)))))

(defn paranoic [& msgs]
  (when (can-log :paranoic)
    (@logger :paranoic (str/join " " (map str msgs)))))
