primitives buffers
writeBufferDouble: anObject index: anIndex value: value
	<primitive: 'primitiveWriteBufferDouble' module: 'PolyglotPlugin'>
	^ (self isBufferWritable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopWriteBufferDouble: anIndex - 1 and: value ] ]
		ifFalse: [ self error: 'Buffer is not writable or does not have buffer elements' ]