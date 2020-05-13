public
openFile

	self fileEntry size > 1000000 ifTrue: [UIManager inform: 'The file is too big to open it.'. ^ self].
	
	self languageInfo: (PELanguageInfo forExtension: self fileEntry extension ifNone: [nil]).
	self selection: nil.
	self code: (self addCodeBoxes: self fileEntry read)