helpers
getSource: anObject
	^ (Interop hasSourceLocation: anObject)
		ifTrue: [ (Interop getSourceLocation: anObject) getCharacters asString ]
		ifFalse: [ 'object has no source location' ].