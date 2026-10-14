primitives
primitiveLastError
	<primitive: 'primitiveGetLastError' module: 'PolyglotPlugin'>
	self isSupportedByVM
		ifTrue: [ self primitiveFailed ]
		ifFalse: [ self error: 'VM support for Polyglot API is not available' ]