running code
polyglotEvaluateCurrentFile

	| polyglot |
	self activeEditArea saveUnsavedCode.
	polyglot := Smalltalk classNamed: #Polyglot.
	(polyglot notNil and: (self currentLanguageInfo isSmalltalk and: PolyglotEditor evaluateSmalltalkDirectly) not)
		ifTrue: [^ [polyglot 
			eval: self currentLanguageInfo polyglotId
			file: self activeEditArea fileEntry fullPath
			inInnerContext: self class evaluateInInnerContext] ifError: [:error |
				self class error: error withLanguage: self currentLanguage]]
		ifFalse: [^ Compiler evaluate: self activeEditArea fileEntry read]