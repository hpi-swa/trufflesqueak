polyglot-api
isPolyglotEvalAllowed: aLanguageID
	^ (self primitiveIsPolyglotEvalAllowed: aLanguageID)
		or: [ aLanguageID == self languageIdSmalltalk ]