primitives buffers
readBufferInt: anObject index: anIndex
	<primitive: 'primitiveReadBufferInt' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBufferInt: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]