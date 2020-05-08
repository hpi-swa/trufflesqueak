accessing
basicSize
	(Interop isString: self) ifTrue: [ ^ (Interop asString: self) basicSize ].
	(Interop hasArrayElements: self) ifTrue: [ ^ Interop getArraySize: self ].
	"The number of indexable fields of fixed-length objects is 0 (see Object>>basicSize)."
	^ 0