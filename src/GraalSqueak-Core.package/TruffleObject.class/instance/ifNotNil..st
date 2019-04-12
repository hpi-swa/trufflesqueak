testing
ifNotNil: ifNotNilBlock
	self isNull ifFalse: [ ^ ifNotNilBlock value ].
	^ self