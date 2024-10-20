testing
testArrayStatistics
	(Polyglot isPolyglotEvalAllowed: Polyglot languageIdSmalltalk) ifFalse: [ ^self ].
	[ TruffleSqueakUtilities arrayStatistics ] on: Error do: [ :e | self fail: e ]