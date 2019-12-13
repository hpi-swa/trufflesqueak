converting
asSmalltalk
	(Interop isBoolean: self) ifTrue: [ ^ Interop asBoolean: self ].
	(Interop isString: self) ifTrue: [ ^ Interop asString: self ].
	(Interop fitsInLong: self) ifTrue: [ ^ Interop asLong: self ].
	(Interop fitsInDouble: self) ifTrue: [ ^ Interop asDouble: self ].
	self error: 'Unable to convert to Smalltalk object'