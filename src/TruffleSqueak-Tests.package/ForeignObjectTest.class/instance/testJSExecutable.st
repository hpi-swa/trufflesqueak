testing
testJSExecutable
	| jsLambda |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsLambda := (Polyglot eval: Polyglot languageIdJS string: 'x => x').
	self assert: (Interop isExecutable: jsLambda).
	self assert: Smalltalk equals: (Interop execute: jsLambda with: {Smalltalk}).
	self assert: Array equals: (jsLambda value: Array).
	
	self assert: 42 equals: (Polyglot eval: Polyglot languageIdJS string: '() => 42') value.
	self assert: (2 + 3) equals: (
		(Polyglot eval: Polyglot languageIdJS string: '(a, b) => a + b') value: 2 value: 3).
	self assert: (2 + 3 + 4) equals: (
	(Polyglot eval: Polyglot languageIdJS string: '(a, b, c) => a + b + c') value: 2 value: 3 value: 4).
	self assert: (2 + 3 + 4 + 5) equals: (
		(Polyglot eval: Polyglot languageIdJS string: '(a, b, c, d) => a + b + c + d') value: 2 value: 3 value: 4 value: 5).
	self assert: (2 + 3 + 4 + 5 + 6 + 7 + 8) equals: (
		(Polyglot eval: Polyglot languageIdJS string: '(a, b, c, d, e, f, g) => a + b + c + d + e + f + g')
			valueWithArguments: #(2 3 4 5 6 7 8)).
