primitives identity
identityHashCode: anObject
	<primitive: 'primitiveIdentityHashCode' module: 'PolyglotPlugin'>
	(self hasIdentity: anObject)
		ifFalse: [ self error: 'Object does not have an identity' ]
		ifTrue: [ self error: 'Unexpected error' ]