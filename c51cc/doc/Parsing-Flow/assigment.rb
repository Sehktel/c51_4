ASSIGNMENT Parsing Flow
------------------------

Input: "x = 10"
        |
        V
[Tokenization]
        |
        +--> ["x", "=", "10"]
               |
               V
[Left Operand Parsing]
               |
               +--> Parses left operand "x"
               |    - Validates as a valid identifier
               |
               V
[Operator Recognition]
               |
               +--> Identifies assignment operator "="
               |
               V
[Right Operand Parsing]
               |
               +--> Parses right operand "10"
               |    - Validates as a literal
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :assignment
               |     :left {
               |       :type :identifier
               |       :name "x"
               |     }
               |     :right {
               |       :type :literal
               |       :value "10"
               |     }
               |     :line 15}
