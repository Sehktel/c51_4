CONSTANT Parsing Flow
----------------------

Input: "const int MAX_VALUE = 100;"
        |
        V
[Tokenization]
        |
        +--> ["const", "int", "MAX_VALUE", "=", "100", ";"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies constant keyword "const"
               |    - Validates against known keywords
               |
               V
[Type Recognition]
               |
               +--> Identifies type "int"
               |    - Validates against known types
               |
               V
[Identifier Validation]
               |
               +--> Parses identifier "MAX_VALUE"
               |    - Validates as a valid identifier
               |
               V
[Assignment Parsing]
               |
               +--> Parses assignment "="
               |    - Validates as an assignment operator
               |
               V
[Value Parsing]
               |
               +--> Parses value "100"
               |    - Validates as a valid literal
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :constant
               |     :data-type "int"
               |     :identifier "MAX_VALUE"
               |     :value 100
               |     :line 5}
