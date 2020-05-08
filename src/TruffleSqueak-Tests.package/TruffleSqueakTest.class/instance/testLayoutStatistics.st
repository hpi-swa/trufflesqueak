testing
testLayoutStatistics
	[ TruffleSqueakUtilities layoutStatistics ] on: Error do: [ :e | self fail: e ]