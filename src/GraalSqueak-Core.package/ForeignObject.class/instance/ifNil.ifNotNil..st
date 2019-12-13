testing
ifNil: nilBlock ifNotNil: ifNotNilBlock
	^ self isNil
		ifTrue: [ nilBlock value ]
		ifFalse: [ ifNotNilBlock value ]