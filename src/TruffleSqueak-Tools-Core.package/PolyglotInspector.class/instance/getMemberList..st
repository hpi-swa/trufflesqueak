helpers
getMemberList: anObject
	| members |
	members := Interop getMembers: anObject.
	^ members
		ifEmpty: [ '<empty list of members>' asText
			addAttribute: TextEmphasis italic ]
		ifNotEmpty: [ | invocables readables remainder |
			invocables := OrderedCollection new.
			readables := OrderedCollection new.
			remainder := OrderedCollection new.
			members sorted do: [ :ea |
				(Interop isMemberInvocable: anObject member: ea)
					ifTrue: [ invocables add: ea ]
					ifFalse: [ (Interop isMemberReadable: anObject member: ea)
						ifTrue: [ readables add: ea ]
						ifFalse: [ remainder add: ea ] ] ].
			Text streamContents: [ :aStream |
				remainder ifNotEmpty: [
					aStream
						withAttributes: { TextEmphasis bold. TextEmphasis italic. TextColor color: Color red }
							do: [ aStream nextPutAll: 'Non-readable Members' ];
						cr;
						nextPutAll: (remainder joinSeparatedBy: ', ');
						cr ].
				readables ifNotEmpty: [
					aStream
						withAttributes: { TextEmphasis bold. TextEmphasis italic }
							do: [ aStream nextPutAll: 'Readable Members' ];
						cr.
					readables do: [ :member | | value |
						value := Interop readMember: anObject member: member.
						aStream
							nextPutAll: member;
							nextPutAll: ': ';
							withAttributes: { TextEmphasis italic. TextColor color: Color gray }
								do: [ aStream nextPutAll: '', value asString ];
							cr].
					aStream cr ].
				invocables ifNotEmpty: [
					aStream
						withAttributes: { TextEmphasis bold. TextEmphasis italic }
							do: [ aStream nextPutAll: 'Invocable Members' ];
						cr;
						nextPutAll: (invocables joinSeparatedBy: ', ');
						cr ] ] ]