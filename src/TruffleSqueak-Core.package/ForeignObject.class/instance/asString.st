converting
asString
	"asString is part of both Squeak's and Truffle's API, so fallback to toDisplayString."
	^ (Interop isString: self)
		ifTrue: [ Interop asString: self ]
		ifFalse: [ Interop toDisplayString: self ]