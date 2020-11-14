testing
testExceptionHandling
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	self assert: ((
		Polyglot
			eval: 'js'
			string: 'try { Polyglot.eval("smalltalk", "42/0") } catch (error) { error }') dividend = 42)