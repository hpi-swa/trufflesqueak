primitives iterator
getIteratorNextElement: anObject
	<primitive: 'primitiveGetIteratorNextElement' module: 'PolyglotPlugin'>
	^ (self isIterator: anObject)
		ifTrue: [ anObject interopGetIteratorNextElement ]
		ifFalse: [ self error: 'Object is not an iterator' ]