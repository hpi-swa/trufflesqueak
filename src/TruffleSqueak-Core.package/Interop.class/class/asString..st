primitives converting
asString: anObject
	<primitive: 'primitiveAsString' module: 'PolyglotPlugin'>
	^ (self isString: anObject)
		ifTrue: [ anObject interopAsString ]
		ifFalse: [ self error: 'Object cannot be converted to string' ]