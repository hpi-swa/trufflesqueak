testing
ifNil: nilBlock
	self isNull ifTrue: [ ^ nilBlock value ].
	^ self