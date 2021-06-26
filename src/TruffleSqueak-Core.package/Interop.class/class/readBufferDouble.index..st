primitives buffers
readBufferDouble: anObject index: anIndex
	<primitive: 'primitiveReadBufferDouble' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferDouble: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]