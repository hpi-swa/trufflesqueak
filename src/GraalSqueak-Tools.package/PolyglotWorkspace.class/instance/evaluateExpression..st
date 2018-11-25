evaluation
evaluateExpression: selection
	self isForeign ifFalse: [ ^ Compiler evaluate: selection asString ].
	^ Polyglot eval: self languageID string: selection asString