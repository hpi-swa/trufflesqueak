accessing
languageID
	
	^ languageID ifNil: [ languageID := Polyglot defaultLanguage ]
