accessing
language: aSymbol

	language := aSymbol.	
	language ifNotNil: [
		self code: (PELineEndingManager 
			with: self innerLanguageInfo defaultLineEndings string: self code).
		self textStyler languageInfo: (PELanguageInfo forId: aSymbol).
		(self innerLanguageInfo isSmalltalk and: [PolyglotEditor smalltalkCodeBoxWarningShown not]) ifTrue: [
			PolyglotEditor showSmalltalkCodeBoxWarning]].
	self changed: #languageButtonLabel.
	self changed: #color.
	self triggerEvent: #codeBoxChanged