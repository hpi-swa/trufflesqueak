primitives
primitiveAsBoolean
	<primitive: 'primitiveAsBoolean' module: 'PolyglotPlugin'>
	self primitiveIsBoolean
		ifFalse: [ self error: 'Object cannot be converted to boolean' ]
		ifTrue: [ self error: 'Unexpected error' ]