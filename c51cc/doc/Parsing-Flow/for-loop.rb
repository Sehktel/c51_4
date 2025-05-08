FOR Loop Parsing Flow
---------------------

Input: "for (int i = 0; i < 10; i++)"
        |
        V
[Tokenization]
        |
        +--> ["for", "(", "int", "i", "=", "0", ";", "i", "<", "10", ";", "i++", ")"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies "for" loop structure
               |
               V
[Initialization Parsing]
               |
               +--> Variable Declaration Stage
               |    - Data type detection
               |    - Variable name
               |    - Initial value assignment
               |    Example: "int i = 0"
               |
               V
[Condition Parsing]
               |
               +--> Logical Expression Evaluation
               |    - Left operand
               |    - Comparison operator
               |    - Right operand
               |    Example: "i < 10"
               |
               V
[Iteration Expression]
               |
               +--> Loop Variable Modification
               |    - Increment/decrement operation
               |    - Variable manipulation
               |    Example: "i++"
               |
               V
[Body Parsing]
               |
               +--> Statement/Block Processing
               |    - Single statement
               |    - Compound statement
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :for
               |     :init {:type "int" 
               |            :var "i" 
               |            :value 0}
               |     :condition {:left "i"
               |                 :op "<"
               |                 :right 10}
               |     :increment "i++"
               |     :body [...]}
