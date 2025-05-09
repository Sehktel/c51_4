UNARY OPERATION Parsing Flow
-----------------------------

Input: "++x"
        |
        V
[Tokenization]
        |
        +--> ["++", "x"]
               |
               V
[Operator Recognition]
               |
               +--> Identifies unary operator "++"
               |    - Validates as a valid unary operation
               |
               V
[Operand Parsing]
               |
               +--> Parses operand "x"
               |    - Validates as a valid identifier
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :unary-operation
               |     :operator "++"
               |     :operand {
               |       :type :identifier
               |       :name "x"
               |     }
               |     :line 5}
