helpers
membersReadableInfoFor: anObject
	^ ((Interop getMembers: anObject)
		select: [ :ea | (Interop isMemberReadable: anObject member: ea) and: [
				(Interop isMemberInvocable: anObject member: ea) not ] ])
			ifNotEmpty: [ :readables | Text streamContents: [ :aStream |
				readables sorted do: [ :member | | value |
					value := [ Interop readMember: self object member: member ]
								on: Error do: [ :e | e ].
					aStream
						withAttribute: (PluggableTextAttribute evalBlock: [ value inspect ])
							do: [ aStream nextPutAll: member ];
						nextPutAll: ': ';
						withAttributes: { TextEmphasis italic. TextColor color: Color gray }
							do: [ aStream nextPutAll: '', value asString ];
						cr ] ] ]
			ifEmpty: [ 'n/a' ]
