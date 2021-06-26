code pane menu
addCustomCodePaneMenuItems: aMenu shifted: aBoolean
	<codePaneMenu>
	<menuPriority: 400>
	
	aMenu
		add: 'select language... (L)'
			target: self
			selector: #selectLanguage;
		add: 'set default language... (h)'
			target: self
			selector: #setDefaultLanguage;
		add: 'set syntax highlighter... (h)'
			target: self
			selector: #setSyntaxHighlighter.

	Smalltalk at: #PolyglotEditor ifPresent: [ :unused |
		aMenu addLine.
		aMenu
			add: 'open in Polyglot Editor'
			target: self
			selector: #openInPolyglotEditor ].

	^ aMenu