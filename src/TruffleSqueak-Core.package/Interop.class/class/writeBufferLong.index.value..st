primitives buffers
writeBufferLong: anObject index: anIndex value: value
	<primitive: 'primitiveWriteBufferLong' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopWriteBufferLong: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]