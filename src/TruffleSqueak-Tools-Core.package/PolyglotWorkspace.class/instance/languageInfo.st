accessing
languageInfo
	
	^ languageInfo ifNil: [ languageInfo := LanguageInfoStub instance ]
