primitives members
writeMember: anObject member: aMember value: aValue
	<primitive: 'primitiveWriteMember' module: 'PolyglotPlugin'>
	^ (self isMemberWritable: anObject member: aMember)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopWriteMember: aMember and: aValue ] ]
		ifFalse: [ self error: 'Member ', aMember, ' is not writable' ]