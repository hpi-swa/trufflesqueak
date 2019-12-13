accessing
basicAt: index
	(Interop isString: self) ifTrue: [ ^ (Interop asString: self) at: index ].
	^ Interop readArrayElement: self index: index