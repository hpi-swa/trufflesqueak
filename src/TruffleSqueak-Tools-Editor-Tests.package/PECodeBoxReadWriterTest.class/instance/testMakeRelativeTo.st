as yet unclassified
testMakeRelativeTo

	| rw |
	rw := PECodeBoxReadWriter.
	
	self assert: './file.txt' equals: (rw make: '/tmp/file.txt' relativeTo: '/tmp').
	self assert: './../file.txt' equals: (rw make: '/file.txt' relativeTo: '/tmp').
	self assert: './../../file.txt' equals: (rw make: '/tmp/file.txt' relativeTo: '/tmp/foo/bar')