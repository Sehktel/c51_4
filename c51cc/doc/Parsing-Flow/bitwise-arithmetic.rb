BITWISE ARITHMETIC Parsing Flow
-------------------------------

Input: "a & b | c"
        |
        V
[Tokenization]
        |
        +--> ["a", "&", "b", "|", "c"]
               |
               V
[Operator Recognition]
               |
               +--> Identifies bitwise operators "&" and "|"
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
               +--> Parses right operand "b | c"
               |    - Recognizes "|" as the main operator
               |    - Parses "b" as left operand
               |    - Parses "c" as right operand
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :bitwise-arithmetic
               |     :operator "|"
               |     :left {
               |       :type :bitwise-arithmetic
               |       :operator "&"
               |       :left {
               |         :type :identifier
               |         :name "a"
               |       }
               |       :right {
               |         :type :identifier
               |         :name "b"
               |       }
               |     }
               |     :right {
               |       :type :identifier
               |       :name "c"
               |     }
               |     :line 15}
