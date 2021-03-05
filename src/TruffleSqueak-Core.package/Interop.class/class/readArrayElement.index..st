primitives arrays
readArrayElement: anObject index: anIndex
	<primitive: 'primitiveReadArrayElement' module: 'PolyglotPlugin'>
	^ (self isArrayElementReadable: anObject index: anIndex)
		ifTrue: [ anObject interopReadArrayElement: anIndex - 1 ]
		ifFalse: [ self error: 'Array element is not readable at index #', anIndex ]