private
evaluateExpression: selection

	| polyglot |
	polyglot := Smalltalk classNamed: #Polyglot.
	^ (polyglot notNil and: [polyglot isPolyglotAccessAllowed])
		ifTrue: [
			[PolyglotEditor evaluateInInnerContext
				ifTrue: [ polyglot evalInInnerContext: self languageInfo polyglotId string: selection asString ]
				ifFalse: [ polyglot eval: self languageInfo polyglotId string: selection asString ]] ifError: [:error |
			PolyglotEditor error: error withLanguage: self languageInfo languageName]]
		ifFalse: [Compiler evaluate: selection asString]