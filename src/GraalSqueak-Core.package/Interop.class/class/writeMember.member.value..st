primitives
writeMember: anObject member: aMember value: aValue
	<primitive: 'primitiveWriteMember' module: 'PolyglotPlugin'>
	(self isMemberWritable: anObject member: aMember)
		ifFalse: [ self error: 'Member ', aMember, ' is not writable' ]
		ifTrue: [ Polyglot signalPolyglotError ]