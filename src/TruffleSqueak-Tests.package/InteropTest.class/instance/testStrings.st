testing
testStrings
	| s |
	(Polyglot isPolyglotEvalAllowed) ifFalse: [ ^self ].
	
	self deny: (Interop isString: Smalltalk).

	s := 'foo'.
	self assert: (Interop isString: s).
	self assert: s equals: (Interop asString: s).
	
	s := s asWideString.
	self assert: (s isKindOf: WideString).
	self assert: (Interop isString: s).
	self assert: s equals: (Interop asString: s).
