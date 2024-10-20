evaluation
evaluateExpression: selection
	^ (Polyglot isPolyglotEvalAllowed: self languageInfo getId)
		ifTrue: [ Polyglot
					evalWithBindings: self languageInfo getId asString
					string: selection asString ]
		ifFalse: [ Compiler evaluate: selection ]