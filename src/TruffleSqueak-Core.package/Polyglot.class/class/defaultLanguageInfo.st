preferences
defaultLanguageInfo
	^ DefaultLanguageInfo ifNil: [ 
		DefaultLanguageInfo := self availableLanguages
			detect: [ :ea | ea getId = self languageIdSmalltalk ]
			ifNone: [ self error: 'Failed to find Smalltalk language info' ] ]