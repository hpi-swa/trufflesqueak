primitives
asBoolean: anObject
	<primitive: 'primitiveAsBoolean' module: 'PolyglotPlugin'>
	(self isBoolean: anObject)
		ifFalse: [ self error: 'Object cannot be converted to boolean' ]
		ifTrue: [ self error: 'Unexpected error' ]