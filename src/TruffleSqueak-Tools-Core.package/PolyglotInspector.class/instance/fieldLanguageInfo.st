fields
fieldLanguageInfo

	^ (self newFieldForType: #misc key: #language)
		name: 'language info'; emphasizeName;
		valueGetter: [ :o | self getLanguageInfo: o ]; printValueAsIs;
		valueGetterExpression: 'Interop getLanguageInfo: self';
		yourself