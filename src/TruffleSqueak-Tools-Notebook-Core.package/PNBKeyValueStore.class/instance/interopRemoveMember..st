overrides
interopRemoveMember: member
	^ self dictionary removeKey: member asString ifAbsent: nil 