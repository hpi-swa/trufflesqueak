accessing
basicAt: index
	self primitiveIsString ifTrue: [ ^ self primitiveAsString at: index ].
	^ self primitiveReadArrayElement: index