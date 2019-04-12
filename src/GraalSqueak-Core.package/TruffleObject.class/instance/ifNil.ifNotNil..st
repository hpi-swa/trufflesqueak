testing
ifNil: nilBlock ifNotNil: ifNotNilBlock
	^ self isNull
		ifTrue: [ nilBlock value ]
		ifFalse: [ ifNotNilBlock value ]