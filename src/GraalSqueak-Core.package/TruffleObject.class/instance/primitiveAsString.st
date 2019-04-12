primitives
primitiveAsString
	<primitive: 'primitiveAsString' module: 'PolyglotPlugin'>
	self primitiveIsString
		ifFalse: [ self error: 'Object cannot be converted to string' ]
		ifTrue: [ self error: 'Unexpected error' ]