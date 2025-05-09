IDENTIFIER Parsing Flow
------------------------

Input: "variableName"
        |
        V
[Tokenization]
        |
        +--> ["variableName"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies identifier structure
               |    - Validates against naming conventions
               |    - Checks for reserved keywords
               |
               V
[Name Validation]
               |
               +--> Ensures identifier is not a keyword
               |    - Checks against list of reserved words
               |    - Validates format (e.g., no special characters)
               |
               V
[Symbol Table Check]
               |
               +--> Verifies identifier existence in symbol table
               |    - Ensures identifier has been declared
               |    - Checks for scope resolution
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :identifier
               |     :name "variableName"
               |     :scope "currentScope"
               |     :line 10}