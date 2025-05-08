DO-WHILE Loop Parsing Flow
--------------------------

Input: "do { x++; } while (x < 10)"
        |
        V
[Tokenization]
        |
        +--> ["do", "{", "x++", ";", "}", "while", "(", "x", "<", "10", ")", ";"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies "do-while" unique structure
               |    - Distinguishes from other loops
               |    - Requires post-condition execution
               |
               V
[Body Parsing (First Phase)]
               |
               +--> Initial Statement Block
               |    - Guaranteed first execution
               |    - Can contain multiple statements
               |    - Enclosed in compound statement {}
               |    Example: "{ x++; }"
               |
               V
[Condition Parsing]
               |
               +--> Post-Execution Condition
               |    - Evaluated after body execution
               |    - Determines loop continuation
               |    - Supports complex boolean expressions
               |    Example: "x < 10"
               |
               V
[Condition Components]
               |
               +--> Detailed Condition Analysis
               |    - Left operand
               |    - Comparison/Logical operator
               |    - Right operand
               |    - Potential nested conditions
               |
               V
[Structural Characteristics]
               |
               +--> Unique Loop Properties
               |    - Body always executes at least once
               |    - Condition checked after execution
               |    - Differs from while/for loop semantics
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :do-while
               |     :body [... statements ...]
               |     :condition {:left "x"
               |                 :op "<"
               |                 :right 10}
               |     :min-iterations 1}
