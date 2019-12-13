primitives
asInstant: anObject
	<primitive: 'primitiveAsInstant' module: 'PolyglotPlugin'>
	(self isInstant: anObject)
		ifFalse: [ self error: 'Object cannot be converted to instant' ]
		ifTrue: [ self error: 'Unexpected error' ]