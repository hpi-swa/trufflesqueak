*TruffleSqueak-Tools-Editor-Core
addModelItemsToWindowMenu: aMenu

	Smalltalk at: #PolyglotEditor ifPresent: [ :unused |
		aMenu
			add: 'open in Polyglot Editor'
			target: self
			selector: #openInPolyglotEditor.
		aMenu addLine ].

	super addModelItemsToWindowMenu: aMenu