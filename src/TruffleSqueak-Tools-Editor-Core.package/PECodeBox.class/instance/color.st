accessing
color

	^ self language 
		ifNotNil: [self innerLanguageInfo color]
		ifNil: [self windowColorToUse]