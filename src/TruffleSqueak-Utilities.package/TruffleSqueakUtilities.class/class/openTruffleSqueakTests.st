world menu
openTruffleSqueakTests
	| suite |
	suite := TestSuite named: 'TruffleSqueak'.
	((SystemOrganizer default classesIn: 'TruffleSqueak-Tests')
		select: [ :ea | ea isTestClass ] thenCollect: [ :ea | ea buildSuite ])
			do: [ :ea | suite addTests: ea tests ].
	TestRunner openForSuite: suite