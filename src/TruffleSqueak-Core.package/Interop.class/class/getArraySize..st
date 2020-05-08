primitives arrays
getArraySize: anObject
	<primitive: 'primitiveGetArraySize' module: 'PolyglotPlugin'>
	(self hasArrayElements: anObject)
		ifFalse: [ self error: 'Object does not have array elements' ]
		ifTrue: [ self error: 'Unexpected error' ]