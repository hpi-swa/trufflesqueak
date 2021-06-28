evaluation
evaluateExpression: selection
	^ Polyglot isPolyglotEvalAllowed
		ifTrue: [ Polyglot
					evalWithBindings: self languageInfo getId asString
					string: selection asString ]
		ifFalse: [ Compiler evaluate: selection ]