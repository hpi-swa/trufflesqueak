testing
ifNotNil: ifNotNilBlock ifNil: nilBlock 
	^ self isNil
		ifTrue: [ nilBlock value ]
		ifFalse: [ ifNotNilBlock value ]