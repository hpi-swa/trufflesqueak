polyglot-api
languageVersionOf: aLanguageID
	^ (self primitiveGetLanguageInfo: aLanguageID)
		ifNotNil: [:l | l third]
		ifNil: [self error: 'Could not find ', aLanguageID]