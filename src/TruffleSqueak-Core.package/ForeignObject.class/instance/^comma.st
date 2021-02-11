arithmetic
, other
	self isString ifTrue: [ ^ self asString, other ].
	(Interop hasArrayElements: self) ifTrue: [ ^ self asArray, other ].
	^ super, other