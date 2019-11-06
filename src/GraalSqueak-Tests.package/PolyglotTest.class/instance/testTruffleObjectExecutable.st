testing
testTruffleObjectExecutable
	| jsLambda |
	jsLambda := (Polyglot eval: Polyglot languageIdJS string: 'x => x').
	self assert: jsLambda primitiveIsExecutable.
	self assert: Smalltalk equals: (jsLambda primitiveExecuteWithArguments: {Smalltalk}).
