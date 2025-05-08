WHILE Loop Parsing Flow
-----------------------

Input: "while (x > 0)"
        |
        V
[Tokenization]
        |
        +--> ["while", "(", "x", ">", "0", ")"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies "while" loop structure
               |
               V
[Condition Parsing]
               |
               +--> Comprehensive Logical Expression
               |    - Evaluation of entire condition
               |    - Multiple potential forms:
               |      * Simple comparison
               |      * Complex boolean expression
               |      * Function call
               |    Example: "x > 0"
               |
               V
[Condition Components]
               |
               +--> Detailed Condition Breakdown
               |    - Left operand
               |    - Comparison/Logical operator
               |    - Right operand
               |    - Potential nested conditions
               |
               V
[Body Parsing]
               |
               +--> Statement Processing
               |    - Single statement execution
               |    - Compound statement block
               |    - Nested control structures
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :while
               |     :condition {:left "x"
               |                 :op ">"
               |                 :right 0}
               |     :body [...]}
