helpers
longPrintStringFor: anObject
	((Interop hasMembers: anObject) and:
		[ (Interop getMemberSize: anObject) > 0 ])
			ifFalse: [ ^ 'object has no members' ].
	^  String streamContents: [:aStream |
		(Interop getMembers: anObject) sorted doWithIndex:
			[ :member :index |
			aStream nextPutAll: member;
			nextPut: $:;
		 	space;
		 	tab.
			(Interop readMember: anObject member: member) printOn: aStream.
		 	aStream cr ] ]