primitives meta-data
getMetaSimpleName: anObject
	<primitive: 'primitiveGetMetaSimpleName' module: 'PolyglotPlugin'>
	(self isMetaObject: anObject)
		ifFalse: [ self error: 'Object is not a meta object' ]
		ifTrue: [ self error: 'Unexpected error' ]