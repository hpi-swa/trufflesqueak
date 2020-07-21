helpers
getMemberList: anObject
	^ String streamContents: [:aStream |
		(Interop getMembers: anObject) sorted doWithIndex:
			[ :member :index |
			aStream nextPutAll: member;
			nextPut: $:;
		 	space;
		 	tab.
			(Interop readMember: anObject member: member) printOn: aStream.
		 	aStream cr ] ]