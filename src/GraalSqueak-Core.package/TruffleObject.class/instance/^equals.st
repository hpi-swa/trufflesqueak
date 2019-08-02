comparing
= anObject
	self primitiveIsBoolean ifTrue: [ ^ self primitiveAsBoolean = anObject ].
	self primitiveFitsInLong ifTrue: [ ^ self primitiveAsLong = anObject ].
	self primitiveFitsInDouble ifTrue: [ ^ self primitiveAsDouble = anObject ].
	self primitiveIsString ifTrue: [ ^ self primitiveAsString = anObject ].
	^ super = anObject