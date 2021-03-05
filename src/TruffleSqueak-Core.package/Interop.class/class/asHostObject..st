primitives internal
asHostObject: aGuestObject
	<primitive: 'primitiveToHostObject' module: 'PolyglotPlugin'>
	(self isHostObject: self)
		ifFalse: [ self error: 'Object not a host object' ]
		ifTrue: [ self error: 'Unexpected error' ]