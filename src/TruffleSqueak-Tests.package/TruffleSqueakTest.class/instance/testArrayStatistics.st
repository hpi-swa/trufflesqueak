testing
testArrayStatistics
	[ TruffleSqueakUtilities arrayStatistics ] on: Error do: [ :e | self fail: e ]