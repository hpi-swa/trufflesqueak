testing
testPolyglotEval
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].
	self assert: Polyglot primitiveIsPolyglotEvalAllowed.
	self assert: 7 equals: (Polyglot eval: Polyglot languageIdJS string: '1 + 2 * 3').
	self assert: (42 min: 21) equals: (Polyglot eval: Polyglot languageIdJS string: 'Math.min(42, 21)').

	self should: [ Polyglot eval: Polyglot languageIdJS string: 'Polyglot eval: "js" string: "1 + 2"' ] raise: Error.
	self should: [ Polyglot eval: Polyglot languageIdJS string: 'const _testPolyglotEval = 42; const _testPolyglotEval = 42; _testPolyglotEval' ] raise: Error.

	"`global` is disabled by default, use `console` instead"
	self assert: 42 equals: (Polyglot eval: Polyglot languageIdJS string: 'console._testPolyglotEval = 42').
	self assert: (Polyglot evalInInnerContext: Polyglot languageIdJS string: 'console._testPolyglotEval') isNil.
	self assert: 21 equals: (Polyglot evalInInnerContext: Polyglot languageIdJS string: 'console._testPolyglotEval = 21').
	self assert: 42 equals: (Polyglot eval: Polyglot languageIdJS string: 'console._testPolyglotEval').
	self assert: true equals: (Polyglot eval: Polyglot languageIdJS string: 'delete console._testPolyglotEval').
	self assert: (Polyglot eval: Polyglot languageIdJS string: 'console._testPolyglotEval') isNil.

	self assert: 21 equals: (Polyglot evalInInnerContext: Polyglot languageIdJS string: 'const _testPolyglotEvalOther = 21; _testPolyglotEvalOther').
	self should: [ Polyglot eval: Polyglot languageIdJS string: '_testPolyglotEvalOther' ] raise: Error.
	self assert: 21 equals: (Polyglot evalInInnerContext: Polyglot languageIdJS string: 'let _testPolyglotEvalOther = 21; _testPolyglotEvalOther').