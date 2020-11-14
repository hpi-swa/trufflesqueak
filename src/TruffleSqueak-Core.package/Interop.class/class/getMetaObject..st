primitives meta-data
getMetaObject: anObject
	<primitive: 'primitiveGetMetaObject' module: 'PolyglotPlugin'>
	(self hasMetaObject: anObject)
		ifFalse: [ self error: 'Object does not have a meta object' ]
		ifTrue: [ self error: 'Unexpected error' ]