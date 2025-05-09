BREAK Parsing Flow
-------------------

Input: "break;"
        |
        V
[Tokenization]
        |
        +--> ["break", ";"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies break statement
               |    - Validates presence of 'break' keyword
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :break
               |     :line 20}
