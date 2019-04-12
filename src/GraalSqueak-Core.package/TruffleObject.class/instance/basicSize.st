accessing
basicSize
	self isString ifTrue: [ ^ self asString basicSize ].
	^ self primitiveGetArraySize