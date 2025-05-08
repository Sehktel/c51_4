Switch-Case Parsing Flow
------------------------

Input: "switch (expression)"
        |
        V
[Tokenization]
        |
        +--> ["switch", "(", "expression", ")", "{", ... "}"]
               |
               V
[Keyword Recognition]
               |
               +--> Identification of switch structure
               |
               V
[Condition Parsing]
               |
               +--> Switch expression analysis
               |
               V
[Case Blocks Parsing]
               |
               +--> Multiple Case Processing
               |    |
               |    +--> Regular Case Branches
               |    |    - Constant labels
               |    |    - Specific value matching
               |    |
               |    +--> [DEFAULT CASE DETECTION]
               |         |
               |         +--> Explicit "default:" keyword
               |         |    - Optional in switch
               |         |    - Catches unmatched cases
               |         |    - Always last in sequence
               |
               V
[Default Case Handling]
               |
               +--> Specific Processing
               |    - Separate parsing path
               |    - Fallback execution block
               |    - Unique AST representation
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :switch
               |     :condition {:expr "status"}
               |     :cases [
               |       {:label 1 :body [...]}
               |       {:label 2 :body [...]}
               |       {:label :default 
               |        :body [...]}
               |     ]
               |    }