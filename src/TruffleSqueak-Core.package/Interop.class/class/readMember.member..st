primitives members
readMember: anObject member: aMember
	<primitive: 'primitiveReadMember' module: 'PolyglotPlugin'>
	(self isMemberReadable: anObject member: aMember)
		ifFalse: [ self error: 'Member ', aMember, ' is not readable' ]
		ifTrue: [ self primitiveFailed ]