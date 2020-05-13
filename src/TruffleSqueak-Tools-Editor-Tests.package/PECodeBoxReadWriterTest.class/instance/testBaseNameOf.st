as yet unclassified
testBaseNameOf

	| rw |
	rw := PECodeBoxReadWriter.
	
	self assert: 'foo.txt' equals: (rw baseNameOf: '/tmp/foo.txt')