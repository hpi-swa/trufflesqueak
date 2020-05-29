comparing
= anObject
	self == anObject ifTrue: [ ^ true ].
	(Interop isBoolean: self) ifTrue: [ ^ anObject = (Interop asBoolean: self) ].
	(Interop fitsInLong: self) ifTrue: [ ^ anObject = (Interop asLong: self) ].
	(Interop fitsInDouble: self) ifTrue: [ ^ anObject = (Interop asDouble: self) ].
	(Interop isString: self) ifTrue: [ ^ anObject = (Interop asString: self) ].
	(Interop isHostObject: self) ifTrue: [ ^ anObject = (Interop asHostObject: self) ].
	^ false