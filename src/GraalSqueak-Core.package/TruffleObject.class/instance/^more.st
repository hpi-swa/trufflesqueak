comparing
> anObject
	self primitiveFitsInLong ifTrue: [ ^ self primitiveAsLong > anObject ].
	self primitiveFitsInDouble ifTrue: [ ^ self primitiveAsDouble > anObject ].
	self primitiveIsString ifTrue: [ ^ self primitiveAsString > anObject ].
	self error: 'Object not comparable'