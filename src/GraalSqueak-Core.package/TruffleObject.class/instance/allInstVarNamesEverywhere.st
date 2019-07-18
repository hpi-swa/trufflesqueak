class behavior
allInstVarNamesEverywhere
	^ self primitiveHasMembers
		ifTrue: [ self primitiveGetMembers sorted ]
		ifFalse: [ #() ]