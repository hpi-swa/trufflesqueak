accessing
size
	<primitive: 'primitiveGetArraySize' module: 'PolyglotPlugin'>
	self isString ifTrue: [ ^ self asString basicSize ].
	self primitiveHasArrayElements ifFalse: [self errorNotIndexable].
	^ 0