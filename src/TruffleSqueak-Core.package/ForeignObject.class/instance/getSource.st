source code management
getSource
	^ (Interop hasSourceLocation: self)
		ifFalse: [ nil ]
		ifTrue: [ (Interop getSourceLocation: self) getCharacters asString ]