testing
testDurations
	(Polyglot isPolyglotEvalAllowed) ifFalse: [ ^self ].
	
	self deny: (Interop isDuration: Smalltalk).
	
	self assert: (Interop isDuration: Duration oneDay).
	self assert: Duration oneDay equals: (Interop asDuration: Duration oneDay) asDuration.