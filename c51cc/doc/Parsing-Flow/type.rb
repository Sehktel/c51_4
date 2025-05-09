TYPE Parsing Flow
-----------------

Input: "int myVariable"
        |
        V
[Tokenization]
        |
        +--> ["int", "myVariable"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies type keyword "int"
               |    - Validates against known types (e.g., int, float, char)
               |
               V
[Identifier Validation]
               |
               +--> Parses identifier "myVariable"
               |    - Validates as a valid identifier
               |    - Checks for naming conventions
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :type-declaration
               |     :data-type "int"
               |     :identifier "myVariable"
               |     :line 5}
