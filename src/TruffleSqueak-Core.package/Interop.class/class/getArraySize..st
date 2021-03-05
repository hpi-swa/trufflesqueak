primitives arrays
getArraySize: anObject
	<primitive: 'primitiveGetArraySize' module: 'PolyglotPlugin'>
	^ (self hasArrayElements: anObject)
		ifTrue: [ anObject interopGetArraySize ]
		ifFalse: [ self error: 'Object does not have array elements' ]