testing
testExceptionHandling
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	self assert: ((
		Polyglot
			eval: 'js'
			string: 'try { Polyglot.eval("smalltalk", "1/0") } catch (error) { error }') isKindOf: ZeroDivide)