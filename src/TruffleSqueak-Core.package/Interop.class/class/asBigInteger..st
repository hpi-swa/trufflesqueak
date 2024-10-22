primitives numbers
asBigInteger: anObject
	<primitive: 'primitiveAsBigInteger' module: 'PolyglotPlugin'>
	^ (self fitsInBigInteger: anObject)
		ifTrue: [ anObject interopAsBigInteger ]
		ifFalse: [ self error: 'Object does not fit in BigInteger' ]