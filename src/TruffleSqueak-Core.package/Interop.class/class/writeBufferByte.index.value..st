primitives buffers
writeBufferByte: anObject index: anIndex value: value
	<primitive: 'primitiveWriteBufferByte' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopWriteBufferByte: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]