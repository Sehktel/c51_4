BINARY EXPRESSION Parsing Flow
-------------------------------

Input: "a = b + c * d - g"
        |
        V
[Tokenization]
        |
        +--> ["a", "=", "b", "+", "c", "*", "d", "-", "g"]
               |
               V
[Operator Recognition]
               |
               +--> Identifies assignment operator '='
               |    - Recognizes binary operators '+', '*', and '-'
               |
               V
[Left Operand Parsing]
               |
               +--> Parses left operand "a"
               |    - Validates as a valid identifier
               |
               V
[Right Operand Parsing]
               |
               +--> Parses right operand "b + c * d - g"
               |    - Recognizes '-' as the main operator
               |    - Parses "b + c * d" as left operand
               |    - Parses "g" as right operand
               |
               V
[Left Operand of Right Expression Parsing]
               |
               +--> Parses "b + c * d"
               |    - Recognizes '+' as the main operator
               |    - Parses "b" as left operand
               |    - Parses "c * d" as right operand
               |
               V
[Right Operand of Right Expression Parsing]
               |
               +--> Parses "c * d"
               |    - Recognizes '*' as the operator
               |    - Parses "c" as left operand
               |    - Parses "d" as right operand
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :assignment
               |     :left {
               |       :type :identifier
               |       :name "a"
               |     }
               |     :right {
               |       :type :binary-expression
               |       :operator "="
               |       :left {
               |         :type :binary-expression
               |         :operator "-"
               |         :left {
               |           :type :binary-expression
               |           :operator "+"
               |           :left {
               |             :type :identifier
               |             :name "b"
               |           }
               |           :right {
               |             :type :binary-expression
               |             :operator "*"
               |             :left {
               |               :type :identifier
               |               :name "c"
               |             }
               |             :right {
               |               :type :identifier
               |               :name "d"
               |             }
               |           }
               |         }
               |         :right {
               |           :type :identifier
               |           :name "g"
               |         }
               |       }
               |     }
               |     :line 10}


               BINARY EXPRESSION Parsing Flow
               -------------------------------
               
               Input: "f += 5;"
                       |
                       V
               [Tokenization]
                       |
                       +--> ["f", "+=", "5", ";"]
                              |
                              V
               [Operator Recognition]
                              |
                              +--> Identifies compound assignment operator '+='
                              |
                              V
               [Left Operand Parsing]
                              |
                              +--> Parses left operand "f"
                              |    - Validates as a valid identifier
                              |
                              V
               [Right Operand Parsing]
                              |
                              +--> Parses right operand "5"
                              |    - Validates as a literal
                              |
                              V
               [Structured Representation]
                              |
                              +--> {
                              |     :type :assignment
                              |     :left {
                              |       :type :identifier
                              |       :name "f"
                              |     }
                              |     :right {
                              |       :type :binary-expression
                              |       :operator "+="
                              |       :left {
                              |         :type :identifier
                              |         :name "f"
                              |       }
                              |       :right {
                              |         :type :literal
                              |         :value "5"
                              |       }
                              |     }
                              |     :line 15}