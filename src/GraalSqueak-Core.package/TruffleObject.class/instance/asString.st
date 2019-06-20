overrides
asString
	"asString is part of both Squeak's and Truffle's API, so fallback to primitiveStringRepresentation."
	^ self primitiveIsString
		ifTrue: [ self primitiveAsString ]
		ifFalse: [ self primitiveStringRepresentation ]