user interface
addModelItemsToWindowMenu: aMenu
	aMenu addLine.
	Polyglot availableLanguages do: [:ea |
		aMenu
			add: 'select ', ea getName ,'...'
			target: self
			selector: #languageInfo:
			argument: ea ].
	aMenu addLine.

	Smalltalk at: #PolyglotEditor ifPresent: [ :unused |
		aMenu
			add: 'open in Polyglot Editor'
			target: self
			selector: #openInPolyglotEditor.
		aMenu addLine ].

	super addModelItemsToWindowMenu: aMenu