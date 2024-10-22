primitives meta-data
getMetaParents: anObject
	<primitive: 'primitiveGetMetaParents' module: 'PolyglotPlugin'>
	^ (self hasMetaParents: anObject)
		ifTrue: [ anObject interopGetMetaParents ]
		ifFalse: [ self error: 'Object has no meta parents' ]