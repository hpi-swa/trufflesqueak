testing
testNulls
	(Polyglot isPolyglotEvalAllowed) ifFalse: [ ^self ].
	
	self deny: (Interop isNull: Smalltalk).
	self deny: (Interop isNull: UndefinedObject).
	self assert: (Interop isNull: nil)
