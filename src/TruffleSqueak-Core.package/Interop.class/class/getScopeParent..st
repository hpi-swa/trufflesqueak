primitives scope
getScopeParent: anObject
	<primitive: 'primitiveGetScopeParent' module: 'PolyglotPlugin'>
	^ (self hasScopeParent: anObject)
		ifTrue: [ anObject interopGetScopeParent ]
		ifFalse: [ self error: 'Object does not have a scope parent' ]