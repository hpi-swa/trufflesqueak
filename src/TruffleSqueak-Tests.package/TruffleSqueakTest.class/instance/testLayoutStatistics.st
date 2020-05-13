testing
testLayoutStatistics
	Polyglot isPolyglotEvalAllowed ifFalse: [ ^self ].
	[ TruffleSqueakUtilities layoutStatistics ] on: Error do: [ :e | self fail: e ]