convenience
asSmalltalk
	self primitiveIsBoolean ifTrue: [ ^ self primitiveAsBoolean ].
	self primitiveIsString ifTrue: [ ^ self primitiveAsString ].
	self primitiveFitsInLong ifTrue: [ ^ self primitiveAsLong ].
	self primitiveFitsInDouble ifTrue: [ ^ self primitiveAsDouble ].
	self error: 'Unable to convert to Smalltalk object'