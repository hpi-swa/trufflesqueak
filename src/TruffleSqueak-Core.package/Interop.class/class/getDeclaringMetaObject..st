primitives meta-data
getDeclaringMetaObject: anObject
	<primitive: 'primitiveGetDeclaringMetaObject' module: 'PolyglotPlugin'>
	^ (self hasDeclaringMetaObject: anObject)
		ifTrue: [ anObject interopGetDeclaringMetaObject ]
		ifFalse: [ self error: 'Object does not have a declaring meta object' ]