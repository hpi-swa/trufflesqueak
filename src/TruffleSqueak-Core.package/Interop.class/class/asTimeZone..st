primitives converting
asTimeZone: anObject
	<primitive: 'primitiveAsTimeZone' module: 'PolyglotPlugin'>
	(self isTimeZone: anObject)
		ifFalse: [ self error: 'Object cannot be converted to time zone' ]
		ifTrue: [ self error: 'Unexpected error' ]