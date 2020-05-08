primitives arrays
readArrayElement: anObject index: anIndex
	<primitive: 'primitiveReadArrayElement' module: 'PolyglotPlugin'>
	(self isArrayElementReadable: anObject index: anIndex)
		ifFalse: [ self error: 'Array element is not readable at index #', anIndex ]
		ifTrue: [ self primitiveFailed ]