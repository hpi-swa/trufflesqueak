testing
testTruffleObjectBasic
	self assert: (Interop isNull: (Polyglot eval: Polyglot languageIdJS string: 'null')).
	
	self assert: (Interop isInstantiable: (Polyglot eval: 'js' string: 'Object')).
	self assert: 0 equals: (Interop getMemberSize:
		(Interop instantiate: (Polyglot eval: 'js' string: 'Object') with: #()))