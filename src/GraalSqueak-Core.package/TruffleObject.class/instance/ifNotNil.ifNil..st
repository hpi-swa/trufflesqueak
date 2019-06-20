testing
ifNotNil: ifNotNilBlock ifNil: nilBlock 
	^ self primitiveIsNull
		ifTrue: [ nilBlock value ]
		ifFalse: [ ifNotNilBlock value ]