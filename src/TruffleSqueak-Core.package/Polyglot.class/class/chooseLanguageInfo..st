helpers
chooseLanguageInfo: aTitle
	| languages |
	languages := self availableLanguages.
	^ UIManager default 
			chooseFrom: (languages collect: [ :ea | ea getName asString ])
			values: languages
			title: aTitle