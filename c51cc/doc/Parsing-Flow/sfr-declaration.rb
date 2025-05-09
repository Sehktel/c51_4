SFR DECLARATION Parsing Flow
-----------------------------

Input: "sfr mySFR = 0x20"
        |
        V
[Tokenization]
        |
        +--> ["sfr", "mySFR", "=", "0x20"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies SFR declaration structure
               |    - Validates presence of 'sfr' keyword
               |    - Checks for identifier and value
               |
               V
[Identifier and Value Validation]
               |
               +--> Ensures identifier is valid and value is a valid address
               |    - Validates against naming conventions
               |    - Checks for valid hexadecimal format
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :sfr-declaration
               |     :name "mySFR"
               |     :address "0x20"
               |     :line 15}
