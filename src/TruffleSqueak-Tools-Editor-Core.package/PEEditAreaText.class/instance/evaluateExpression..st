private
evaluateExpression: selection

	| polyglot |
	polyglot := Smalltalk classNamed: #Polyglot.
	^ (polyglot notNil and: [polyglot isPolyglotAccessAllowed])
		ifTrue: [
			[polyglot
				eval: self languageInfo polyglotId
				string: selection asString
				inInnerContext: PolyglotEditor evaluateInInnerContext] ifError: [:error |
			PolyglotEditor error: error withLanguage: self languageInfo languageName]]
		ifFalse: [Compiler evaluate: selection asString]