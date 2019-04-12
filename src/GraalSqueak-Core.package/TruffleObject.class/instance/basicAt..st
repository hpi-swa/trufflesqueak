accessing
basicAt: index
	self isString ifTrue: [ ^ self asString at: index ].
	^ self primitiveReadArrayElement: index