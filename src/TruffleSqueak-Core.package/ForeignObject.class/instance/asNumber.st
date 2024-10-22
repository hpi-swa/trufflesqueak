converting
asNumber
	(Interop fitsInLong: self) ifTrue: [ ^ Interop asLong: self ].
	(Interop fitsInDouble: self) ifTrue: [ ^ Interop asDouble: self ].
	(Interop fitsInBigInteger: self) ifTrue: [ ^ Interop asBigInteger: self ].
	self error: 'Unable to interpret foreign value as number'