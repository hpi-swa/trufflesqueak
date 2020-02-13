testing
testSmalltalkDNU
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	self assert: 1234 equals: (
		Polyglot
			eval: 'js'
			string: 'Polyglot.eval("smalltalk", "InteropTestDNUTestSubject new").freedom()').
	self assert: 1 equals: (
		Polyglot
			eval: 'js'
			string: 'Polyglot.eval("smalltalk", "InteropTestDNUTestSubject new").freedom_("some argument")').
	self assert: 2 equals: (
		Polyglot
			eval: 'js'
			string: 'Polyglot.eval("smalltalk", "InteropTestDNUTestSubject new").freedom_and_(true, [])').