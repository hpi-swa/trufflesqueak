primitives converting
asBoolean: anObject
	<primitive: 'primitiveAsBoolean' module: 'PolyglotPlugin'>
	^ (self isBoolean: anObject)
		ifTrue: [ anObject interopAsBoolean ]
		ifFalse: [ self error: 'Object cannot be converted to boolean' ]