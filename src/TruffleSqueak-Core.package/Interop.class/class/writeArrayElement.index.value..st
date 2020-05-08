primitives arrays
writeArrayElement: anObject index: anIndex value: aValue
	<primitive: 'primitiveWriteArrayElement' module: 'PolyglotPlugin'>
	(self isArrayElementWritable: anObject index: anIndex)
		ifFalse: [ self error: 'Array element is not writable at index #', anIndex ]
		ifTrue: [ Polyglot signalPolyglotError ]