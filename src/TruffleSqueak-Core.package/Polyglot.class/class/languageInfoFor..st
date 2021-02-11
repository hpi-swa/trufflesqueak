polyglot-api
languageInfoFor: aLanguageID
	^ (self primitiveGetInternalLanguageInfo: aLanguageID)
		ifNotNil: [ :info | info ]
		ifNil: [ self error: 'Could not find ', aLanguageID ]