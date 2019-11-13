testing
testTruffleObjectExecutable
	| jsLambda |
	jsLambda := (Polyglot eval: Polyglot languageIdJS string: 'x => x').
	self assert: (Interop isExecutable: jsLambda).
	self assert: Smalltalk equals: (Interop execute: jsLambda with: {Smalltalk}).
