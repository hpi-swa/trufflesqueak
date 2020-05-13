testing
testArrayStatistics
	Polyglot isPolyglotEvalAllowed ifFalse: [ ^self ].
	[ TruffleSqueakUtilities arrayStatistics ] on: Error do: [ :e | self fail: e ]