LABEL Parsing Flow
-------------------

Input: "labelName:"
        |
        V
[Tokenization]
        |
        +--> ["labelName", ":"]
               |
               V
[Identifier Validation]
               |
               +--> Parses label "labelName"
               |    - Validates as a valid identifier
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :label
               |     :name "labelName"
               |     :line 15}
