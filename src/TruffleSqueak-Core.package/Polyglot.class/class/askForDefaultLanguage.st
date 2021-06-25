default language
askForDefaultLanguage
	| languages |
	languages := Polyglot availableLanguages.
	defaultLanguage := UIManager default 
		chooseFrom: (languages collect: [ :ea | ea getName ])
		values: (languages collect: [ :ea | ea getId asString asSymbol ])
		title: 'Which language would you like to use by default?'.
	^ defaultLanguage.