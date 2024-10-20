testing
testLayoutStatistics
	(Polyglot isPolyglotEvalAllowed: Polyglot languageIdSmalltalk) ifFalse: [ ^self ].
	[ TruffleSqueakUtilities layoutStatistics ] on: Error do: [ :e | self fail: e ]