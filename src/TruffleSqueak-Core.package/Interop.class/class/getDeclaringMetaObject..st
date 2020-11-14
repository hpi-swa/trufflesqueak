primitives meta-data
getDeclaringMetaObject: anObject
	<primitive: 'primitiveGetDeclaringMetaObject' module: 'PolyglotPlugin'>
	(self hasDeclaringMetaObject: anObject)
		ifFalse: [ self error: 'Object does not have a declaring meta object' ]
		ifTrue: [ self error: 'Unexpected error' ]