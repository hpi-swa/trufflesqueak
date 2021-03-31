primitives buffers
readBufferShort: anObject index: anIndex
	<primitive: 'primitiveReadBufferShort' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopReadBufferShort: anIndex - 1 ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]