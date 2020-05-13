as yet unclassified
testParentFolderOf

	| rw |
	rw := PECodeBoxReadWriter.
	
	self assert: '/tmp' equals: (rw parentFolderOf: '/tmp/foo.txt')