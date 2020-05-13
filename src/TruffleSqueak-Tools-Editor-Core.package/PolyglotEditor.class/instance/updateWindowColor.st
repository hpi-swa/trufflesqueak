window color
updateWindowColor

	self topView setWindowColor: (self currentLanguageInfo 
		ifNotNil: [:languageInfo | languageInfo color] 
		ifNil: [self class defaultWindowColor])