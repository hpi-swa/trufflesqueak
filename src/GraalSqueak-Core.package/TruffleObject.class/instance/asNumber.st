converting
asNumber
	self primitiveFitsInLong ifTrue: [ ^ self primitiveAsLong ].
	self primitiveFitsInDouble ifTrue: [ ^ self primitiveAsDouble ].
	self error: 'Unable to interpret foreign value as number'