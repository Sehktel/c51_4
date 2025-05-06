(ns c51cc.logger)

;; (require '[clojure.string :as str])

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

;; (defonce logger (atom (fn [msg] (println (str "[" (name @current-log-level) "] " msg)))))
(defonce logger (atom (fn [msg] (println msg))))

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

(defn log [msg]
  (when (can-log @current-log-level)
    (@logger msg)))

(defn debug [msg]
  (when (can-log :debug)
    (@logger msg)))

(defn error [msg]
  (when (can-log :error)
    (@logger msg)))

(defn warning [msg]
  (when (can-log :warning)
    (@logger msg)))

(defn info [msg]
  (when (can-log :info)
    (@logger msg)))

(defn debug [msg]
  (when (can-log :debug)
    (@logger msg)))

(defn trace [msg]
  (when (can-log :trace)
    (@logger msg)))

