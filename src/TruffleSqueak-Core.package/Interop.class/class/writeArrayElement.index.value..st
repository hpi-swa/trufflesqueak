primitives arrays
writeArrayElement: anObject index: anIndex value: aValue
	<primitive: 'primitiveWriteArrayElement' module: 'PolyglotPlugin'>
	^ (self isArrayElementWritable: anObject index: anIndex)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteArrayElement: anIndex - 1 and: aValue ] ]
		ifFalse: [ self error: 'Array element is not writable at index #', anIndex ]