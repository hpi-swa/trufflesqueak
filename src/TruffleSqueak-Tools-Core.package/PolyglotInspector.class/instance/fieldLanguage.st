fields
fieldLanguage

	^ (self newFieldForType: #misc key: #language)
		name: 'language'; emphasizeName;
		valueGetter: [:object | self getLanguageInfo: object ]; printValueAsIs;
		valueGetterExpression: 'Interop getLanguageInfo: self';
		yourself