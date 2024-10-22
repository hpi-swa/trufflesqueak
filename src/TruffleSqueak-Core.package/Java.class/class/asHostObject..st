primitives internal
asHostObject: aGuestObject
	<primitive: 'primitiveToHostObject' module: 'PolyglotPlugin'>
	(Interop isHostObject: aGuestObject)
		ifFalse: [ self error: 'Object not a host object' ]
		ifTrue: [ self error: 'Unexpected error' ]