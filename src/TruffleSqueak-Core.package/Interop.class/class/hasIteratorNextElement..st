primitives iterator
hasIteratorNextElement: anObject
	<primitive: 'primitiveHasIteratorNextElement' module: 'PolyglotPlugin'>
	^ (self isIterator: anObject)
		ifTrue: [ anObject interopHasIteratorNextElement ]
		ifFalse: [ self error: 'Object is not an iterator' ]