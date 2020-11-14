primitives scope
getScopeParent: anObject
	<primitive: 'primitiveGetScopeParent' module: 'PolyglotPlugin'>
	(self hasScopeParent: anObject)
		ifFalse: [ self error: 'Object does not have a scope parent' ]
		ifTrue: [ self error: 'Unexpected error' ]