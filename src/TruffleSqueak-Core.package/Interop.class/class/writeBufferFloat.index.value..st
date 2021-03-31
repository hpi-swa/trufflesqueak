primitives buffers
writeBufferFloat: anObject index: anIndex value: value
	<primitive: 'primitiveWriteBufferFloat' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopWriteBufferFloat: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]