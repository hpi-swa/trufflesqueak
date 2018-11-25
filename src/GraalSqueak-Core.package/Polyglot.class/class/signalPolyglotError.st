helpers
signalPolyglotError
	self primitiveLastError
		ifNotNil: [ :e | self error: e ]
		ifNil: [ self primitiveFailed ]