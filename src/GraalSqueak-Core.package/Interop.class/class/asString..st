primitives
asString: anObject
	<primitive: 'primitiveAsString' module: 'PolyglotPlugin'>
	(self isString: self)
		ifFalse: [ self error: 'Object cannot be converted to string' ]
		ifTrue: [ self error: 'Unexpected error' ]