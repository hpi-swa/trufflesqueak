testing
testArrayStatistics
	[ GraalSqueakUtilities arrayStatistics ] on: Error do: [ :e | self fail: e ]