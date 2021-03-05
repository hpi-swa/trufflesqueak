primitives members
readMember: anObject member: aMember
	<primitive: 'primitiveReadMember' module: 'PolyglotPlugin'>
	^ (self isMemberReadable: anObject member: aMember)
		ifTrue: [ anObject interopReadMember: aMember ]
		ifFalse: [ self error: 'Member ', aMember, ' is not readable' ]