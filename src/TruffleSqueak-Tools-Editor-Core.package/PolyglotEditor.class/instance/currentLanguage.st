current information
currentLanguage

	^ self currentLanguageInfo 
		ifNotNil:[:languageInfo | languageInfo languageName] 
		ifNil: ['unknown language']