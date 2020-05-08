class behavior
allInstVarNamesEverywhere
	^ (Interop hasMembers: self)
		ifTrue: [ (Interop getMembers: self) sorted ]
		ifFalse: [ #() ]