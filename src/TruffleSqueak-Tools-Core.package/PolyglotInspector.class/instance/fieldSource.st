fields
fieldSource

	^ (self newFieldForType: #misc key: #sourceLocation)
		name: 'source'; emphasizeName;
		valueGetter: [ :o | (Interop getSourceLocation: o) getCharacters asString ]; printValueAsIs;
		valueGetterExpression: 'Interop getSourceLocation: self';
		yourself