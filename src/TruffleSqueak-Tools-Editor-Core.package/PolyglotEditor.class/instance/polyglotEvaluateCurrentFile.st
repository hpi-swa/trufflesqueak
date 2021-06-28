running code
polyglotEvaluateCurrentFile

	| polyglot |
	self activeEditArea saveUnsavedCode.
	polyglot := Smalltalk classNamed: #Polyglot.
	^ (polyglot notNil and: (self currentLanguageInfo isSmalltalk and: PolyglotEditor evaluateSmalltalkDirectly) not)
		ifTrue: [[ | id file |
				id := self currentLanguageInfo polyglotId.
				file := self activeEditArea fileEntry fullPath.
				self class evaluateInInnerContext
					ifTrue: [ polyglot evalInInnerContext: id file: file ]
					ifFalse: [ polyglot eval: id file: file ] ] ifError: [:error |
				self class error: error withLanguage: self currentLanguage]]
		ifFalse: [Compiler evaluate: self activeEditArea fileEntry read]