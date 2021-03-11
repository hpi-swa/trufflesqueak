primitives members
invokeMember: anObject member: aMember arguments: arguments
	<primitive: 'primitiveInvokeMember' module: 'PolyglotPlugin'>
	^ (self isMemberInvocable: anObject member: aMember)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopInvokeMember: aMember and: arguments ] ]
		ifFalse: [ self error: 'Member ', aMember, ' is not invocable' ]