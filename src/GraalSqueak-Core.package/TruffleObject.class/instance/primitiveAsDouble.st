primitives
primitiveAsDouble
	<primitive: 'primitiveAsDouble' module: 'PolyglotPlugin'>
	self primitiveFitsInDouble
		ifFalse: [ self error: 'Object does not fit in double' ]
		ifTrue: [ self error: 'Unexpected error' ]