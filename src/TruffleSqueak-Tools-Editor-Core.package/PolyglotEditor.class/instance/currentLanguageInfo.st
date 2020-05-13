current information
currentLanguageInfo
	^ self activeEditArea fileEntry 
		ifNil: [nil]
		ifNotNil: [:entry | PELanguageInfo forExtension: entry extension ifNone: [nil]]