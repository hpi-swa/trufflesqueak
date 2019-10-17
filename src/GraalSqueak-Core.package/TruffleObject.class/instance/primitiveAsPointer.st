primitives
primitiveAsPointer
	<primitive: 'primitiveAsPointer' module: 'PolyglotPlugin'>
	self primitiveIsPointer
		ifFalse: [ self error: 'Object cannot be converted to a pointer' ]
		ifTrue: [ self error: 'Unexpected error' ]