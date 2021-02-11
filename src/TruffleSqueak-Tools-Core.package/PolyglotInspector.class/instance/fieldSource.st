fields
fieldSource

	^ (self newFieldForType: #misc key: #sourceLocation)
		name: 'source'; emphasizeName;
		valueGetter: [:object | (Interop getSourceLocation: object) getCharacters asString ]; printValueAsIs;
		valueGetterExpression: 'Interop getSourceLocation: self';
		yourself