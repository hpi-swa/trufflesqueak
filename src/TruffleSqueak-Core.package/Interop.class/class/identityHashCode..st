primitives identity
identityHashCode: anObject
	<primitive: 'primitiveIdentityHashCode' module: 'PolyglotPlugin'>
	^ (self hasIdentity: anObject)
		ifTrue: [ self identityHash ]
		ifFalse: [ self error: 'Object does not have an identity' ]