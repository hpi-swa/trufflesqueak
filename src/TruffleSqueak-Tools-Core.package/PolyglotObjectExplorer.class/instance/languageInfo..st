language
languageInfo: aLanguageInfo
	languageInfo == aLanguageInfo ifTrue: [ ^ self ].
	languageInfo := aLanguageInfo.
	self changed: #contents