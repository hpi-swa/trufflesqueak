testing
testTestMapConsistency
	| testNames booleanResult |
	(Polyglot isPolyglotEvalAllowed: Polyglot languageIdSmalltalk) ifFalse: [ ^self ].

	testNames := TestCase buildSuite tests collect: [:test |
		test class asString, '>>', test selector].
	booleanResult := (Java type: 'de.hpi.swa.trufflesqueak.test.SqueakTests')
		testTestMapConsistency: testNames.
	self assert: booleanResult description: 'Test failed on Java side'
