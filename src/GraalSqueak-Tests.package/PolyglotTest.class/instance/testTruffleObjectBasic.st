testing
testTruffleObjectBasic
	self assert: (Polyglot eval: Polyglot languageIdJS string: 'null') primitiveIsNull.
	
	self assert: (Polyglot eval: 'js' string: 'Object') primitiveIsInstantiable.
	self assert: 0 equals: ((Polyglot eval: 'js' string: 'Object') primitiveInstantiate: #()) primitiveGetMemberSize