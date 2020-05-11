testing
testSmalltalkFromJS
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	self assert: Morph equals: (Polyglot eval: Polyglot languageIdJS string: 'Polyglot.eval("smalltalk", "Morph")').

	"Ensure objects instantiated through interop are correctly initialized (see #112)"
	self assert: Morph new color equals:
		(Polyglot eval: Polyglot languageIdJS string: 'new (Polyglot.eval("smalltalk", "Morph"))().color()')