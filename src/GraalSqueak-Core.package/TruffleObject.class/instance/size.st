accessing
size
	<primitive: 'primitiveGetArraySize' module: 'PolyglotPlugin'>
	self primitiveIsString ifTrue: [ ^ self primitiveAsString basicSize ].
	self primitiveHasArrayElements ifFalse: [ self errorNotIndexable ].
	^ 0