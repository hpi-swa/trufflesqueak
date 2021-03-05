primitives meta-data
getMetaQualifiedName: anObject
	<primitive: 'primitiveGetMetaQualifiedName' module: 'PolyglotPlugin'>
	^ (self isMetaObject: anObject)
		ifTrue: [ anObject interopGetMetaQualifiedName ]
		ifFalse: [ self error: 'Object is not a meta object' ]