class behavior
allInstVarNames
	^ self hasMembers
		ifTrue: [ self getMembers ]
		ifFalse: [ #() ]