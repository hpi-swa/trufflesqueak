primitives members
isMemberExisting: anObject member: aMember
	<primitive: 'primitiveIsMemberExisting' module: 'PolyglotPlugin'>
	^ (anObject interopIsMemberReadable: aMember) or: [
		(anObject interopIsMemberModifiable: aMember) or: [
			(anObject interopIsMemberRemovable: aMember) or: [
				(anObject interopIsMemberInvocable: aMember) ] ] ]