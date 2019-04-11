polyglot-api
languageNameOf: aLanguageID
	^ (self primitiveGetLanguageInfo: aLanguageID)
		ifNotNil: [:l | l second]
		ifNil: ["self error: 'Could not find ', aLanguageID" aLanguageID ]