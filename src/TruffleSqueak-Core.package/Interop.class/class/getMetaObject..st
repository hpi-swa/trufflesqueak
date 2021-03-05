primitives meta-data
getMetaObject: anObject
	<primitive: 'primitiveGetMetaObject' module: 'PolyglotPlugin'>
	^ (self hasMetaObject: anObject)
		ifTrue: [ anObject interopGetMetaObject ]
		ifFalse: [ self error: 'Object does not have a meta object' ]