fields
fieldLanguageInfo

	^ (self newFieldForType: #misc key: #language)
		name: 'language info'; emphasizeName;
		valueGetter: [ :o | self languageInfoFor: o ]; printValueAsIs;
		valueGetterExpression: 'Interop getLanguageInfo: self';
		yourself