language
languageInfo: aLanguageInfo
	languageInfo == aLanguageInfo ifTrue: [ ^ self ].
	languageInfo := aLanguageInfo.
	self changed: #style.
	self changed: #relabel