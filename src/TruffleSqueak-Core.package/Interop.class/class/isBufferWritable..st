primitives buffers
isBufferWritable: anObject
	<primitive: 'primitiveIsBufferWritable' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopIsBufferWritable ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]