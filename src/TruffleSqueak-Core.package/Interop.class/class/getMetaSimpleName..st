primitives meta-data
getMetaSimpleName: anObject
	<primitive: 'primitiveGetMetaSimpleName' module: 'PolyglotPlugin'>
	^ (self isMetaObject: anObject)
		ifTrue: [ anObject interopGetMetaSimpleName ]
		ifFalse: [ self error: 'Object is not a meta object' ]