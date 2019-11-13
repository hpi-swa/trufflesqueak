converting
asString
	"asString is part of both Squeak's and Truffle's API, so fallback to primitiveStringRepresentation."
	^ (Interop isString: self)
		ifTrue: [ Interop asString: self ]
		ifFalse: [ Interop stringRepresentation: self ]