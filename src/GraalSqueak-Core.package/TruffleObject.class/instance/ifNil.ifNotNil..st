testing
ifNil: nilBlock ifNotNil: ifNotNilBlock
	^ self primitiveIsNull
		ifTrue: [ nilBlock value ]
		ifFalse: [ ifNotNilBlock value ]