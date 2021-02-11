instance creation
with: anObject member: aMember
	^ self
			with: ((Interop isMemberReadable: anObject member: aMember)
				ifTrue: [ Interop readMember: anObject member: aMember ]
				ifFalse: [ nil ])
			name: aMember
			model: anObject