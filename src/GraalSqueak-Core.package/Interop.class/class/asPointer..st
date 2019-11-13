primitives
asPointer: anObject
	<primitive: 'primitiveAsPointer' module: 'PolyglotPlugin'>
	(self isPointer: self)
		ifFalse: [ self error: 'Object cannot be converted to a pointer' ]
		ifTrue: [ self error: 'Unexpected error' ]