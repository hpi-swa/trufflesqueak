testing
ifNotNil: ifNotNilBlock
	self primitiveIsNull ifFalse: [ ^ ifNotNilBlock value ].
	^ self