comparing
= anObject
	(Interop isBoolean: self) ifTrue: [ ^ (Interop asBoolean: self) = anObject ].
	(Interop fitsInLong: self) ifTrue: [ ^ (Interop asLong: self) = anObject ].
	(Interop fitsInDouble: self) ifTrue: [ ^ (Interop asDouble: self) = anObject ].
	(Interop isString: self) ifTrue: [ ^ (Interop asString: self) = anObject ].
	^ super = anObject