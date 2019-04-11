printing
printOn: aStream
	| title |
	title := self primitiveIsBoxed
		ifTrue: [ self primitiveUnbox ]
		ifFalse: [ self primitiveStringRepresentation ].
	aStream nextPutAll: title