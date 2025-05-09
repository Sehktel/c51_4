STATEMENT Parsing Flow
------------------------

Input: "if (condition) { statement; }"
        |
        V
[Tokenization]
        |
        +--> ["if", "(", "condition", ")", "{", "statement", ";", "}"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies statement structure
               |    - Distinguishes between control flow statements
               |    - Recognizes block statements
               |
               V
[Condition Parsing]
               |
               +--> Parses condition expression
               |    - Validates syntax of condition
               |    - Ensures proper operator usage
               |
               V
[Body Parsing]
               |
               +--> Parses statement block
               |    - Checks for opening and closing braces
               |    - Accumulates statements within the block
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :if-statement
               |     :condition {...}
               |     :body [... statements ...]
               |     :line 5}
