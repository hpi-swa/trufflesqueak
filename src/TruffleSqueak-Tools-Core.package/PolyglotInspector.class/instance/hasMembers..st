helpers
hasMembers: anObject
	^ (Interop hasMembers: anObject) and: [ (Interop getMemberSize: anObject) > 0 ]