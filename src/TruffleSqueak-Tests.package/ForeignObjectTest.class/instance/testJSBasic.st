testing
testJSBasic
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].
	self assert: (Interop isNull: (Polyglot eval: Polyglot languageIdJS string: 'null')).
	
	self assert: (Interop isInstantiable: (Polyglot eval: Polyglot languageIdJS string: 'Object')).
	self assert: 0 equals: (Interop getMemberSize:
		(Interop instantiate: (Polyglot eval: Polyglot languageIdJS string: 'Object') with: #()))