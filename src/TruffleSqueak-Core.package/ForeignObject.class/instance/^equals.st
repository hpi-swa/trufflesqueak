comparing
= anObject
	self == anObject ifTrue: [ ^ true ].
	(Interop isBoolean: self) ifTrue: [ ^ anObject = (Interop asBoolean: self) ].
	(Interop fitsInLong: self) ifTrue: [ ^ anObject = (Interop asLong: self) ].
	(Interop fitsInDouble: self) ifTrue: [ ^ anObject = (Interop asDouble: self) ].
	(Interop fitsInBigInteger: self) ifTrue: [ ^ anObject = (Interop asBigInteger: self) ].
	(Interop isString: self) ifTrue: [ ^ anObject = (Interop asString: self) ].
	((Interop isHostObject: self) and: [ Interop isHostObject: anObject ])
		ifTrue: [ ^ self equals: anObject ].
	^ false