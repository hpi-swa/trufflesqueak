accessing
at: identifier put: value
	^ identifier isInteger
		ifTrue: [ self primitiveWriteArrayElement: identifier value: value ]
		ifFalse: [ self primitiveWriteMember: identifier value: value ]