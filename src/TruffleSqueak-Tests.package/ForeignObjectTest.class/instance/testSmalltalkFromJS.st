testing
testSmalltalkFromJS
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	self assert: Morph equals: (self jsEval: 'Polyglot.eval("smalltalk", "Morph")').

	"Ensure objects instantiated through interop are correctly initialized (see trufflesqueak#112)"
	self assert: Morph new color equals:
		(self jsEval: 'new (Polyglot.eval("smalltalk", "Morph"))().color()').
	
	"Ensure _ is optional at end of messages to Smalltalk (see trufflesqueak#114)"
	self assert: 2 equals:
		(self jsEval: 'Polyglot.eval(''smalltalk'', ''Array'').with_with_(42, true).length').
	self assert: 2 equals:
		(self jsEval: 'Polyglot.eval(''smalltalk'', ''Array'').with_with(42, true).length')