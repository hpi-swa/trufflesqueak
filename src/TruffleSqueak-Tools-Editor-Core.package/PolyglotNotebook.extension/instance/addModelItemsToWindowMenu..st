*TruffleSqueak-Tools-Editor-Core
addModelItemsToWindowMenu: aMenu

	aMenu addLine.
	aMenu
		addUpdating: #toggleShowLanguageScopesLabel
		target: self
		action: #toggleShowLanguageScopes.

	Smalltalk at: #PolyglotEditor ifPresent: [ :unused |
		aMenu addLine.
		aMenu
			add: 'open in Polyglot Editor'
			target: self
			selector: #openInPolyglotEditor ].

	super addModelItemsToWindowMenu: aMenu