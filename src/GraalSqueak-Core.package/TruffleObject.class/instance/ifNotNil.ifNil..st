testing
ifNotNil: ifNotNilBlock ifNil: nilBlock 
	^ self isNull
		ifTrue: [ nilBlock value ]
		ifFalse: [ ifNotNilBlock value ]