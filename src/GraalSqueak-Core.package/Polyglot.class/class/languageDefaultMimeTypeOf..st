polyglot-api
languageDefaultMimeTypeOf: aLanguageID
	^ (self primitiveGetLanguageInfo: aLanguageID)
		ifNotNil: [:l | l fourth]
		ifNil: [self error: 'Could not find ', aLanguageID]