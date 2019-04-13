accessing
at: identifier
	^ identifier isInteger
		ifTrue: [ self primitiveReadArrayElement: identifier ]
		ifFalse: [ self primitiveReadMember: identifier ]