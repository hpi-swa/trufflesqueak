world menu
adjustAppsMenu
	TheWorldMenu registry removeAll.
	PreferenceBrowser registerInOpenMenu.
	#(CallTargetBrowser GraalInfoMorph
		PolyglotEditor PolyglotNotebook PolyglotWorkspace
		SquitBrowser
		TestRunner
		TruffleSqueakUtilities)
			do: [ :ea | Smalltalk at: ea ifPresent: [ :cls | cls registerInWorldMenu] ].
