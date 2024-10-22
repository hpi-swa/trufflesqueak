testing
testNumbers
	| li |
	self deny: (Interop fitsInLong: Smalltalk).
	self deny: (Interop fitsInDouble: Smalltalk).
	self deny: (Interop fitsInBigInteger: Smalltalk).

	self assert: (Interop fitsInLong: 42).
	self assert: 42 == (Interop asLong: 42).
	self assert: (Interop fitsInDouble: 42).
	self assert: 42.0 == (Interop asDouble: 42).
	
	self deny: (Interop fitsInLong: 42.3).
	self assert: (Interop fitsInDouble: 42.3).
	self assert: 42.3 == (Interop asDouble: 42.3).
	
	li := SmallInteger maxVal + 1.
	self deny: (Interop fitsInLong: li).
	self assert: (Interop fitsInBigInteger: li).
	self assert: li equals: (Interop asBigInteger: li)
