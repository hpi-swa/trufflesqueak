printing
printOn: aStream
	| title |
	title := self primitiveIsString
		ifTrue: [ self primitiveAsString ]
		ifFalse: [ self primitiveStringRepresentation ].
	aStream nextPutAll: title