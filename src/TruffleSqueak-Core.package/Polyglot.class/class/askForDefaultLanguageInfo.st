preferences
askForDefaultLanguageInfo
	| languages |
	languages := Polyglot availableLanguages.
	self defaultLanguageInfo: (
		UIManager default 
			chooseFrom: (languages collect: [ :ea | ea getName ])
			values: languages
			title: 'Which language would you like to use by default?' )