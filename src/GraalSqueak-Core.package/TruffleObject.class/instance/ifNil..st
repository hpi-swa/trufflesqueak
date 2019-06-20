testing
ifNil: nilBlock
	self primitiveIsNull ifTrue: [ ^ nilBlock value ].
	^ self