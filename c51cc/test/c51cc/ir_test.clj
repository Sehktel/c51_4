(ns c51cc.ir-test
  (:require [clojure.test :refer :all]
            [c51cc.ir :as ir]))

(deftest test-ir-program-creation
  (let [program (ir/IRProgram. [])]
    (is (= [] (ir/ir-program-functions program)))))

(deftest test-ir-function-creation
  (let [function (ir/IRFunction. "main" [] [])]
    (is (= "main" (ir/ir-function-name function)))))

(deftest test-ir-block-creation
  (let [block (ir/IRBlock. [])]
    (is (= [] (ir/ir-block-statements block)))))

(deftest test-ir-return-stmt-creation
  (let [stmt (ir/IRReturnStmt. (ir/IRIntLiteral. 0))]
    (is (= (ir/IRIntLiteral. 0) (ir/ir-return-stmt-expr stmt)))))
    
