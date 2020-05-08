primitives converting
asDate: anObject
	<primitive: 'primitiveAsDate' module: 'PolyglotPlugin'>
	(self isDate: anObject)
		ifFalse: [ self error: 'Object cannot be converted to date' ]
		ifTrue: [ self error: 'Unexpected error' ]