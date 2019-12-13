accessing
size
	(Interop hasArrayElements: self) ifTrue: [ ^ Interop getArraySize: self ].
	(Interop isString: self) ifTrue: [ ^ (Interop asString: self) basicSize ].
	self errorNotIndexable