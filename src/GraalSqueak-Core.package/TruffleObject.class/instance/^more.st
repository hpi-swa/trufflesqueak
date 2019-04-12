comparing
> anObject
	self fitsInLong ifTrue: [ ^ self asLong > anObject ].
	self fitsInDouble ifTrue: [ ^ self asDouble > anObject ].
	self isString ifTrue: [ ^ self asCharacterOrString > anObject ].
	self error: 'Object not comparable'