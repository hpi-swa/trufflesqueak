public
ensurePolyglotIsImported

	| header |
	header := self languageInfo codeForPolyglotHeader.
	(self unsavedCode beginsWith: header) ifFalse: [ | str |
		str := header, self lineEndings.
		self unsavedCode prepend: str.
		self changed: #unsavedCode.
		self selection: self selection + str size]