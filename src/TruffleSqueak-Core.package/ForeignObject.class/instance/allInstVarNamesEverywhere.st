class behavior
allInstVarNamesEverywhere
	^ (Interop hasMembers: self)
		ifTrue: [
			((Interop getMembers: self)
				select: [ :ea | Interop isMemberReadable: self member: ea ])
					sorted ]
		ifFalse: [ #() ]