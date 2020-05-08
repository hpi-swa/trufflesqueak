accessing
runtime
	^ runtime ifNil: [ runtime := self primitiveGetTruffleRuntime ]