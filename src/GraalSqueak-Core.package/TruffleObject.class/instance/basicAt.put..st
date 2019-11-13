accessing
basicAt: index put: value
	(Interop isString: self) ifTrue: [ self error: 'Not supported' ].
	^ Interop writeArrayElement: self index: index value: value