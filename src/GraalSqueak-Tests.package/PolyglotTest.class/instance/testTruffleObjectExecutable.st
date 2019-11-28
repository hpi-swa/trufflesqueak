testing
testTruffleObjectExecutable
	| jsLambda |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsLambda := (Polyglot eval: Polyglot languageIdJS string: 'x => x').
	self assert: (Interop isExecutable: jsLambda).
	self assert: Smalltalk equals: (Interop execute: jsLambda with: {Smalltalk}).
