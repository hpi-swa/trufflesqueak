testing
ifNil: nilBlock
	self isNil ifTrue: [ ^ nilBlock value ].
	^ self