menus
codePaneMenu: aMenu shifted: shifted
	aMenu
		add: 'select language... (L)'
			target: self
			selector: #selectLanguage;
		add: 'set adaptive language selection... (l)'
			target: self
			selector: #setAdaptiveLanguageSelection;
		add: 'set default language... (h)'
			target: self
			selector: #setDefaultLanguage;
		add: 'set syntax highlighter... (h)'
			target: self
			selector: #setSyntaxHighlighter.
	^ StringHolder codePaneMenu: aMenu shifted: shifted