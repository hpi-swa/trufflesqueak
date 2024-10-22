primitives meta-data
hasMetaParents: anObject
	<primitive: 'primitiveHasMetaParents' module: 'PolyglotPlugin'>
	^ (self isMetaObject: anObject) 
		ifTrue: [ anObject interopHasMetaParents ]
		ifFalse: [ self error: 'Object is not a meta object' ]