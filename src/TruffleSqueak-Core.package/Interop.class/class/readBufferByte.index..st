primitives buffers
readBufferByte: anObject index: anIndex
	<primitive: 'primitiveReadBufferByte' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferByte: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]