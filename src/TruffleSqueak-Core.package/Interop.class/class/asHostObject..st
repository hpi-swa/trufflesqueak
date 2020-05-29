primitives host
asHostObject: anObject
	"Careful: The host object may not be a proper TruffleObject"
	<primitive: 'primitiveAsHostObject' module: 'PolyglotPlugin'>
	(self isHostObject: anObject)
		ifFalse: [ self error: 'Object is not an host object' ]
		ifTrue: [ self error: 'Unexpected error' ]