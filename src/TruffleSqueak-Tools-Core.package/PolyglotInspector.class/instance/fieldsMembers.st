fields
fieldsMembers
	| invocables readables remainder |
	invocables := OrderedCollection new.
	readables := OrderedCollection new.
	remainder := OrderedCollection new.
	(Interop getMembers: self object) sorted do: [ :ea |
		(Interop isMemberInvocable: self object member: ea)
			ifTrue: [ invocables add: ea ]
			ifFalse: [ (Interop isMemberReadable: self object member: ea)
				ifTrue: [ readables add: ea ]
				ifFalse: [ remainder add: ea ] ] ].
	^ (remainder ifNotEmpty: [ { (self newFieldForType: #misc key: #membersNonReadable)
			name: 'members (non-readable)'; styleName: { Color red }; emphasizeName;
			valueGetter: [:object | remainder joinSeparatedBy: String cr ]; printValueAsIs;
			yourself } ] ifEmpty: [ {} ]),
		(readables ifNotEmpty: [ { (self newFieldForType: #misc key: #membersReadable)
			name: 'members (readable)'; emphasizeName;
			valueGetter: [:object | 
				Text streamContents: [ :aStream |
					readables do: [ :member | | value |
						value := Interop readMember: self object member: member.
						aStream
							withAttribute: (PluggableTextAttribute evalBlock: [ value inspect ])
								do: [ aStream nextPutAll: member ];
							nextPutAll: ': ';
							withAttributes: { TextEmphasis italic. TextColor color: Color gray }
								do: [ aStream nextPutAll: '', value asString ];
							cr] ] ]; printValueAsIs;
			yourself } ] ifEmpty: [ {} ]),
		(invocables ifNotEmpty: [ { (self newFieldForType: #misc key: #membersInvocable)
			name: 'members (invocable)'; emphasizeName;
			valueGetter: [:object | 
				String streamContents: [ :aStream |
					invocables do: [ :member |
						aStream
							nextPutAll: member;
							nextPutAll: ((Interop isMemberReadable: self object member: member)
							ifTrue: [ ' (readable)' ]
							ifFalse: [ ' (non-readable)' ]);
							cr] ] ]; printValueAsIs;
			yourself } ] ifEmpty: [ {} ])