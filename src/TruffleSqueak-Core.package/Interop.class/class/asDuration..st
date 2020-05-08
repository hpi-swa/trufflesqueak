primitives converting
asDuration: anObject
	<primitive: 'primitiveAsDuration' module: 'PolyglotPlugin'>
	(self isDuration: anObject)
		ifFalse: [ self error: 'Object cannot be converted to duration' ]
		ifTrue: [ self error: 'Unexpected error' ]