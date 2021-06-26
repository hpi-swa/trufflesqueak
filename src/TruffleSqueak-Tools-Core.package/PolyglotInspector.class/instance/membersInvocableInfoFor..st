helpers
membersInvocableInfoFor: anObject
	^ ((Interop getMembers: anObject)
			select: [ :ea | Interop isMemberInvocable: anObject member: ea ])
		ifNotEmpty: [ :invocables | Text streamContents: [ :aStream |
			invocables sorted do: [ :member |
				(Interop isMemberReadable: anObject member: member)
					ifTrue: [ aStream
								withAttribute: (PluggableTextAttribute evalBlock: [
										(Interop readMember: self object member: member) inspect ])
									do: [ aStream nextPutAll: member ];
								withAttributes: { TextEmphasis italic. TextColor color: Color gray }
									do: [ aStream nextPutAll: ' (readable)' ] ]
					ifFalse: [ aStream
								nextPutAll: member;
								withAttributes: { TextEmphasis italic. TextColor color: Color gray }
									do: [ aStream nextPutAll: ' (non-readable)' ] ].
						aStream cr ] ] ]
		ifEmpty: [ 'n/a' ]
		