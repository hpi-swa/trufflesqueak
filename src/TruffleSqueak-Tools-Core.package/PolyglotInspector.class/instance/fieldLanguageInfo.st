fields
fieldLanguageInfo

	^ (self newFieldForType: #misc key: #language)
		name: 'language info'; emphasizeName;
		valueGetter: [:object | self getLanguageInfo: object ]; printValueAsIs;
		valueGetterExpression: 'Interop getLanguageInfo: self';
		yourself