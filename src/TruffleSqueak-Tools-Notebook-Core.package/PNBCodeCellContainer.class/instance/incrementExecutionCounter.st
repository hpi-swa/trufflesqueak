helpers
incrementExecutionCounter
	^ executionCounter := (executionCounter ifNil: [ executionCounter := 0 ]) + 1