class behavior
allInstVarNames
	^ (Interop hasMembers: self)
		ifTrue: [
			((Interop getMembers: self)
				select: [ :ea | (Interop isMemberReadable: self member: ea) and: (
								Interop isMemberInvocable: self member: ea) not ])
					sorted ]
		ifFalse: [ #() ]