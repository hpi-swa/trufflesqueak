testing
testTestMapConsistency
	| mapTestNames mapTestNamesSize imageTestNames imageTestNamesSize testNamesDifference allTestsDifference |
	Polyglot isPolyglotEvalAllowed ifFalse: [ ^self ].

	mapTestNames := (Java type: 'de.hpi.swa.trufflesqueak.test.SqueakTests') rawTestNames asCollection.
	self assertSorted: mapTestNames name: 'test.properties'.
	mapTestNamesSize := mapTestNames size.
	mapTestNames := mapTestNames withoutDuplicates. "returns a Set"
	self assert: mapTestNamesSize equals: mapTestNames size
		description: 'test.properties contains duplicates'.

	imageTestNames := (TestCase buildSuite tests collect: [:test | test class asString, '>>', test selector]).
	imageTestNamesSize := imageTestNames size.
	imageTestNames := imageTestNames withoutDuplicates. "returns a Set"
	self assert: imageTestNamesSize equals: imageTestNames size
		description: 'imageTestNames contains duplicates'.

	testNamesDifference := mapTestNames difference: mapTestNames.
	(mapTestNamesSize = imageTestNamesSize and: [testNamesDifference isEmpty]) ifFalse: [
		self assert: testNamesDifference isEmpty
			description: 'Additional tests in tests.properties: ', testNamesDifference.
		allTestsDifference := imageTestNames difference: mapTestNames.
		self assert: allTestsDifference isEmpty
			description: 'Additional tests in image: ', allTestsDifference ]
