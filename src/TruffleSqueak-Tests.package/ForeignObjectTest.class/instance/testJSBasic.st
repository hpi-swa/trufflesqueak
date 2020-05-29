testing
testJSBasic
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].
	self assert: (Interop isNull: (self jsEval: 'null')).
	
	self assert: (Interop isInstantiable: (self jsEval: 'Object')).
	self assert: 0 equals: (Interop getMemberSize:
		(Interop instantiate: (self jsEval: 'Object') with: #()))