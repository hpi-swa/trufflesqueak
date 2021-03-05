primitives members
isMemberWritable: anObject member: aMember
	<primitive: 'primitiveIsMemberWritable' module: 'PolyglotPlugin'>
	^ (anObject interopIsMemberModifiable: aMember) or: [ anObject interopIsMemberInsertable: aMember ]