fields
fieldMembers

	^ (self newFieldForType: #misc key: #members)
		name: 'members'; emphasizeName;
		valueGetter: [:object | self getMemberList: object ]; printValueAsIs;
		yourself