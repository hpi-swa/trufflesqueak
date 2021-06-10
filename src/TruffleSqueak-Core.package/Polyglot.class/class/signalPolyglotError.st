helpers
signalPolyglotError
	| error |
	error := self primitiveLastError.
	error inspect.
	self error: error asString