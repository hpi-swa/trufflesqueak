accessing
basicAt: index put: value
	^ Interop writeArrayElement: foreignObject index: index value: value