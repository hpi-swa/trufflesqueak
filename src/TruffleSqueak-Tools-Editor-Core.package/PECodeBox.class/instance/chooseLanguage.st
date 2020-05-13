UI functions
chooseLanguage

	| choice languageInfos |
	languageInfos := PELanguageInfo allWithPolyglotSupport.
	choice := UIManager default 
		chooseFrom: (languageInfos collect: #languageName) 
		values: (languageInfos collect: #polyglotId).
	choice ifNil: [^ self].
	self language: choice