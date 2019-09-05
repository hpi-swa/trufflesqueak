primitives
primitiveLastError
	<primitive: 'primitiveGetLastError' module: 'PolyglotPlugin'>
	self primitiveIsPolyglotEvalAllowed ifFalse: [
		self error: 'Polyglot API is not available' ].
	self primitiveFailed