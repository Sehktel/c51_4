RETURN Parsing Flow
------------------------

Input: "return expression;"
        |
        V
[Tokenization]
        |
        +--> ["return", "expression", ";"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies return statement structure
               |    - Validates presence of return keyword
               |    - Checks for expression following return
               |
               V
[Expression Parsing]
               |
               +--> Parses the return expression
               |    - Validates syntax of the expression
               |    - Ensures proper operator usage
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :return
               |     :expression {...}
               |     :line 15}
