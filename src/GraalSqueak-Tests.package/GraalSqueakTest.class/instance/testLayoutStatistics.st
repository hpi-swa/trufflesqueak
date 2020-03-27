testing
testLayoutStatistics
	[ GraalSqueakUtilities layoutStatistics ] on: Error do: [ :e | self fail: e ]