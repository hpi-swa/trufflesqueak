primitives members
invokeMember: anObject member: aMember arguments: arguments
	<primitive: 'primitiveInvokeMember' module: 'PolyglotPlugin'>
	(self isMemberInvocable: anObject member: aMember)
		ifFalse: [ self error: 'Member ', aMember, ' is not invocable' ]
		ifTrue: [ Polyglot signalPolyglotError ]