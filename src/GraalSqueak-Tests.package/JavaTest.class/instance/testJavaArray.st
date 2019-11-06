testing
testJavaArray
	| ints |
	ints := (Java type: 'int[]') new: 2.
	self assert: 2 equals: ints size.
	
	self should: [ ints at: 0 ] raise: Error.
	self assert: 0 equals: (ints at: 1).
	self assert: 0 equals: (ints at: 2).
	self should: [ ints at: 3 ] raise: Error.
	
	self assert: 32 equals: (ints at: 1 put: 32).
	self assert: 32 equals: (ints at: 1).