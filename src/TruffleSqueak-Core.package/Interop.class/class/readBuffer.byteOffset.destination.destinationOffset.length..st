primitives buffers
readBuffer: anObject byteOffset: byteOffset destination: destination destinationOffset: destinationOffset length: length
	<primitive: 'primitiveReadBuffer' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopReadBuffer: byteOffset - 1 destination: destination destinationOffset: destinationOffset - 1 length: length ] ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]