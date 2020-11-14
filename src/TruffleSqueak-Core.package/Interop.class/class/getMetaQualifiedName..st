primitives meta-data
getMetaQualifiedName: anObject
	<primitive: 'primitiveGetMetaQualifiedName' module: 'PolyglotPlugin'>
	(self isMetaObject: anObject)
		ifFalse: [ self error: 'Object is not a meta object' ]
		ifTrue: [ self error: 'Unexpected error' ]