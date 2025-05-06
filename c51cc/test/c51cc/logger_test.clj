(ns c51cc.logger_test)
  (require '[clojure.test :refer :all]
           '[c51cc.logger :as log])

;; TODO: Разобрать, что написала нейросеть

;; Функция для перехвата логов
(defn- capture-log [f]
  (let [logs (atom [])]
    (with-redefs [log/logger 
                  (atom (fn [msg]
                          (swap! logs conj msg)))]
      (f)
      @logs)))

(deftest test-log-levels
  (testing "Проверка уровней логирования"
    (log/set-log-level :error)
    
    (is (false? (log/can-log :info)) 
        "Уровень info не должен логироваться при установке error")
    
    (is (true? (log/can-log :error)) 
        "Уровень error должен логироваться")))

(deftest test-logging-functions
  (testing "Базовые функции логирования"
    (log/set-log-level :debug)
    
    (let [logs (capture-log 
                 #(do 
                    (log/error "Тестовая ошибка")
                    (log/warning "Предупреждение")
                    (log/info "Информация")
                    (log/debug "Отладка")))]
      
      (is (= 4 (count logs)) 
          "Должны быть залогированы все сообщения"))))

(deftest test-log-level-boundaries
  (testing "Границы уровней логирования"
    (let [level-hierarchy {:silent 0
                           :error 1
                           :warning 2
                           :info 3
                           :debug 4
                           :trace 5}]
      (doseq [current-level (keys level-hierarchy)]
        (log/set-log-level current-level)
        
        (is (true? (log/can-log current-level))
            (str "Уровень " current-level " должен логироваться"))
        
        (doseq [[level-name level-value] level-hierarchy]
          (when (< (get level-hierarchy current-level 0) level-value)
            (is (false? (log/can-log level-name))
                (str "Уровень " level-name " не должен логироваться при " current-level))))))))

(deftest test-default-log-level
  (testing "Проверка уровня логирования по умолчанию"
    (is (= :info @log/current-log-level)
        "Дефолтный уровень логирования должен быть :info")))

(deftest test-log-level-change
  (testing "Изменение уровня логирования"
    (let [initial-level @log/current-log-level]
      (log/set-log-level :debug)
      (is (= :debug @log/current-log-level)
          "Уровень логирования должен успешно меняться")
      
      ;; Возвращаем начальное состояние
      (log/set-log-level initial-level))))
