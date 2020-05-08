polyglot-api
languageMimeTypesOf: aLanguageID
	^ (self primitiveGetLanguageInfo: aLanguageID)
		ifNotNil: [:l | l fifth]
		ifNil: [self error: 'Could not find ', aLanguageID]