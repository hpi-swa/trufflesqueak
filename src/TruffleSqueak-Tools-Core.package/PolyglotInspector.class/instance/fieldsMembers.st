fields
fieldsMembers

	^ { (self newFieldForType: #misc key: #membersReadable)
			name: 'members (readable)'; emphasizeName;
			valueGetter: [:object | self membersReadableInfoFor: object ];
			printValueAsIs.
		(self newFieldForType: #misc key: #membersInvocable)
			name: 'members (invocable)'; emphasizeName;
			valueGetter: [:object | self membersInvocableInfoFor: object ];
			printValueAsIs. }
