(ns c51cc.ir)

(defrecord IRProgram [functions])
(defrecord IRFunction [name params body])
(defrecord IRBlock [statements])
(defrecord IRReturnStmt [expr])
(defrecord IRBinaryExpr [operator left right])
(defrecord IRIntLiteral [value])
(defrecord IRIdentifier [name])
(defrecord IRInterruptDecl [number function])
(defrecord IRSfrDecl [name address])
(defrecord IRSbitDecl [name sfr bit])
(defrecord IRPointerType [base-type])
(defrecord IRArrayType [base-type dimensions])
(defrecord IRStructType [name fields])
(defrecord IRStructField [type name])

(defrecord IRState [current-function])

(defrecord IRFunctionDecl [name params return-type body])
(defrecord IRVarDecl [type name value])

