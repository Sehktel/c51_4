SBIT DECLARATION Parsing Flow
------------------------------

Input: "sbit myBit = 0"
        |
        V
[Tokenization]
        |
        +--> ["sbit", "myBit", "=", "0"]
               |
               V
[Keyword Recognition]
               |
               +--> Identifies SBIT declaration structure
               |    - Validates presence of 'sbit' keyword
               |    - Checks for identifier and value
               |
               V
[Identifier and Value Validation]
               |
               +--> Ensures identifier is valid and value is a valid bit position
               |    - Validates against naming conventions
               |    - Checks for valid integer format
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :sbit-declaration
               |     :name "myBit"
               |     :bit 0
               |     :line 20}
