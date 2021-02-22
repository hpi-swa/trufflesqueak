converting
asString
	"Always return a ByteString, because asString is also part of Squeak's API."
	^ Interop asString: (
		self isString
			ifTrue: [ self ]
			ifFalse: [ Interop toDisplayString: self ])