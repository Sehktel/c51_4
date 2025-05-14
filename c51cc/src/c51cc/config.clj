(ns c51cc.config)

(defonce DEBUG false) ;; Глобальная переменная DEBUG

;; Функция для изменения значения DEBUG
(defn set-debug 
  "Функция для изменения значения DEBUG"
  [value]
  (alter-var-root #'DEBUG (constantly value)))
