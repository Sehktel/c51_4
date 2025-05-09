FUNCTION PARAMS Parsing Flow
-----------------------------

Input: "int param1, float param2"
        |
        V
[Tokenization]
        |
        +--> ["int", "param1", ",", "float", "param2"]
               |
               V
[Parameter Recognition]
               |
               +--> Identifies parameter structure
               |    - Validates type and name for each parameter
               |    - Checks for comma separation
               |
               V
[Type and Name Validation]
               |
               +--> Ensures each parameter has a valid type and name
               |    - Validates against known types
               |    - Checks for naming conventions
               |
               V
[Accumulation of Parameters]
               |
               +--> Accumulates parameters into a list
               |    - Handles multiple parameters
               |    - Ensures proper order and structure
               |
               V
[Structured Representation]
               |
               +--> {
               |     :type :function-params
               |     :params [
               |       {:type :int, :name "param1"}
               |       {:type :float, :name "param2"}
               |     ]
               |     :line 20}
