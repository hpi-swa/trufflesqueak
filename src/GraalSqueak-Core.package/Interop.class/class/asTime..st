primitives
asTime: anObject
	<primitive: 'primitiveAsTime' module: 'PolyglotPlugin'>
	(self isTime: anObject)
		ifFalse: [ self error: 'Object cannot be converted to time' ]
		ifTrue: [ self error: 'Unexpected error' ]