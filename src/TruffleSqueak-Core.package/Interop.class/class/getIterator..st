primitives iterator
getIterator: anObject
	<primitive: 'primitiveGetIterator' module: 'PolyglotPlugin'>
	^ (self hasIterator: anObject)
		ifTrue: [ anObject interopGetIterator ]
		ifFalse: [ self error: 'Object does not have an iterator' ]