primitives buffers
readBufferLong: anObject index: anIndex
	<primitive: 'primitiveReadBufferLong' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopReadBufferLong: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]