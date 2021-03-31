primitives buffers
getBufferSize: anObject
	<primitive: 'primitiveGetBufferSize' module: 'PolyglotPlugin'>
	^ (self hasBufferElements: anObject)
		ifTrue: [ anObject interopGetBufferSize ]
		ifFalse: [ self error: 'Object does not have buffer elements' ]