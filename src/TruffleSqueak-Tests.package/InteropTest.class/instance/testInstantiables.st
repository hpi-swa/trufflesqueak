testing
testInstantiables
	(Polyglot isPolyglotEvalAllowed) ifFalse: [ ^self ].
	
	self deny: (Interop isInstantiable: Smalltalk).
	
	{Array. OrderedCollection. String} do: [ :ea |
		self assert: (Interop isInstantiable: ea).
		self assert: ea new equals: (Interop instantiate: ea with: #()).
		self assert: (ea new: 5) equals: (Interop instantiate: ea with: #(5)) ]
	