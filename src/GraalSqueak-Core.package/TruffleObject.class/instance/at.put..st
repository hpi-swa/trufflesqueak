accessing
at: identifier put: value
	^ identifier isInteger
		ifTrue: [ self primitiveWriteArrayElement: identifier ]
		ifFalse: [ self primitiveWriteMember: identifier ]