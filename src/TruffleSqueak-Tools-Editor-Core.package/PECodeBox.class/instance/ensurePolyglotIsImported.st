code-generation
ensurePolyglotIsImported

	| header |
	header := self innerLanguageInfo codeForPolyglotHeader.
	(self unsavedCode beginsWith: header) ifFalse: [ | str |
		str := header, self lineEndings.
		self unsavedCode prepend: str.
		self changed: #unsavedCode.
		self selection: self selection + str size]