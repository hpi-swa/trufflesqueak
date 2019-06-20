accessing
basicAt: index put: value
	self primitiveIsString ifTrue: [ self error: 'Not supported' ].
	^ self primitiveWriteArrayElement: index value: value