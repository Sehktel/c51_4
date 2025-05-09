HANDLE ERROR Parsing Flow
---------------------------

Input: Error encountered at token "unexpected-token"
        |
        V
[Error Logging]
        |
        +--> Logs error message
               |
               +--> "Unexpected token: unexpected-token"
               |
               V
[State Recovery]
        |
        +--> Attempts to recover parser state
               |
               +--> Resets to a known good state or skips to next valid token
               |
               V
[Structured Representation]
        |
        +--> {
        |     :type :error
        |     :message "Unexpected token: unexpected-token"
        |     :line 10}
