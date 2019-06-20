primitives
primitiveLastError
	<primitive: 'primitiveGetLastError' module: 'PolyglotPlugin'>
	self primitiveIsPolyglotAccessAllowed ifFalse: [
		self error: 'Polyglot API is not available' ].
	self primitiveFailed