BINARY EXPRESSION Parsing Flow
-------------------------------

Input: "a + b * c"
        |
        V
[Tokenization]
        |
        +--> ["a", "+", "b", "*", "c"]
               |
               V
[Operator Recognition]
               |
               +--> Identifies binary operator structure
               |    - Distinguishes between operators
               |    - Recognizes operator precedence
               |
               V
[Operand Parsing]
               |
               +--> Parses left and right operands
               |    - Validates syntax of operands
               |    - Ensures proper operator usage
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :binary-expression
               |     :operator "+"
               |     :left {...}
               |     :right {...}
               |     :line 10}
