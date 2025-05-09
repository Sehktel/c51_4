EXPECT TOKEN Parsing Flow
---------------------------

Input: "expected-token" (current token is "expected-token")
        |
        V
[Current Token Check]
        |
        +--> Checks current token type and value
               |
               +--> Is current token "expected-token"?
               |    - Yes: Proceed to advance
               |    - No: Handle error
               |
               V
[Advance Token]
        |
        +--> Moves to the next token in the sequence
               |
               V
[Structured Representation]
        |
        +--> {
        |     :type :expect-token
        |     :expected "expected-token"
        |     :actual "expected-token"
        |     :line 5}
