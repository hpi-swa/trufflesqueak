testing
ifNotNil: ifNotNilBlock
	self isNil ifFalse: [ ^ ifNotNilBlock value ].
	^ self