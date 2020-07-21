primitives meta-data
getSourceLocation: anObject
	<primitive: 'primitiveGetSourceLocation' module: 'PolyglotPlugin'>
	(self hasSourceLocation: anObject)
		ifFalse: [ self error: 'Object has no source location' ]
		ifTrue: [ self primitiveFailed ]