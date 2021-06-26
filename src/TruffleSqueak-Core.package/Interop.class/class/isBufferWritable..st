primitives buffers
isBufferWritable: anObject
	<primitive: 'primitiveIsBufferWritable' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopIsBufferWritable ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]