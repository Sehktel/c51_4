GOTO Parsing Flow
------------------

Input: "goto labelName;"
        |
        V
[Tokenization]
        |
        +--> ["goto", "labelName", ";"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies goto keyword "goto"
               |    - Validates presence of 'goto' keyword
               |
               V
[Label Validation]
               |
               +--> Parses label "labelName"
               |    - Validates as a valid identifier
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :goto
               |     :label "labelName"
               |     :line 10}