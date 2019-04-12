interop
asSmalltalk
	self isBoolean ifTrue: [ ^ self asBoolean ].
	self isString ifTrue: [ ^ self asString ].
	self fitsInLong ifTrue: [ ^ self asLong ].
	self fitsInDouble ifTrue: [ ^ self asDouble ].
	self error: 'Unable to convert to Smalltalk object'