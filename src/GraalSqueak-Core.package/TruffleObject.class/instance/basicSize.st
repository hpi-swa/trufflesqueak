accessing
basicSize
	self primitiveIsString ifTrue: [ ^ self primitiveAsString basicSize ].
	^ self primitiveGetArraySize