accessing
basicSize
	(Interop isString: self) ifTrue: [ ^ (Interop asString: self) basicSize ].
	^ Interop getArraySize: self