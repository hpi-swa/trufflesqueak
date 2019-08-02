testing
isZero
	self primitiveFitsInLong ifTrue: [ ^ self primitiveAsLong isZero ].
	self primitiveFitsInDouble ifTrue: [ ^ self primitiveAsDouble isZero ].
	self error: 'Unable to interpret foreign value as number'