accessing
basicAt: index put: value
	self isString ifTrue: [ self error: 'Not supported' ].
	^ self primitiveWriteArrayElement: index value: value