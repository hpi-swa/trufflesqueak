primitives members
removeMember: anObject member: aMember
	<primitive: 'primitiveRemoveMember' module: 'PolyglotPlugin'>
	^ (self isMemberRemovable: anObject member: aMember)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopRemoveMember: aMember ] ]
		ifFalse: [ self error: 'Member ', aMember, ' is not removable' ]