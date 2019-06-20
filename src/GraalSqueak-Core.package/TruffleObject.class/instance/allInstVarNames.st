class behavior
allInstVarNames
	^ self primitiveHasMembers
		ifTrue: [ self primitiveGetMembers ]
		ifFalse: [ #() ]