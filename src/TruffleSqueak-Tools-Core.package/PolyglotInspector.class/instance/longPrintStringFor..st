helpers
longPrintStringFor: anObject
	^ (self hasMembers: anObject)
		ifTrue: [ self getMemberList: anObject ]
		ifFalse: [ 'object has no members' ]