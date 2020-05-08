testing
isZero
	(Interop fitsInLong: self) ifTrue: [ ^ (Interop asLong: self) isZero ].
	(Interop fitsInDouble: self) ifTrue: [ ^ (Interop asDouble: self) isZero ].
	self error: 'Unable to interpret foreign value as number'