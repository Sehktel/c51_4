C Function Declaration Parsing Flow
------------------------------------

Input: "void main(void)"
        |
        V
[Tokenization]
        |
        +--> ["void", "main", "(", "void", ")", ""]
               |
               V
[Parser State Management]
               |
               +--> Initial State
               |    - tokens: ["void", "main", "(", "void", ")"]
               |    - index: 0
               |
               V
[Return Type Parsing]
               |
               +--> "void"
               |
               V
[Function Name Parsing]
               |
               +--> "main"
               |
               V
[Parameter Parsing]
               |
               +--> Condition: "(void)" or "(param1; param2; ...)"
               |    - Handles zero or multiple parameters
               |    - Supports type and optional name
               |
               V
[Interrupt Handling]
               |
               +--> Optional: "interrupt" + number
               |    - Checks for optional interrupt specification
               |    - Validates numeric interrupt value
               |
               V
[Structured Representation]
               |
               +--> {
               |     :return-type "void"
               |     :name "main"
               |     :parameters []
               |     :interrupt nil
               |     }
