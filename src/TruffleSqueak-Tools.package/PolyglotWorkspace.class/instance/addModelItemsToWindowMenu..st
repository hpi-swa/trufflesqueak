menu commands
addModelItemsToWindowMenu: aMenu
	aMenu addLine.
	Polyglot availableLanguages do: [:ea |
		aMenu
			add: 'select ', (Polyglot languageNameOf: ea) ,'...'
			target: self
			selector: #selectLanguage:
			argument: ea ].
	aMenu addLine.

	super addModelItemsToWindowMenu: aMenu