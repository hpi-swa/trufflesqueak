primitives buffers
readBufferFloat: anObject index: anIndex
	<primitive: 'primitiveReadBufferFloat' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferFloat: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]