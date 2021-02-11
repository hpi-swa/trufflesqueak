helpers
isPermitted: aLanguageID
	^ self primitiveGetPublicLanguages anySatisfy: [ :ea | ea getId = aLanguageID ]