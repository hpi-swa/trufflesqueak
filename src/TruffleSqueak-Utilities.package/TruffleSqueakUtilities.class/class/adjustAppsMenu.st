world menu
adjustAppsMenu
	TheWorldMenu registry removeAll.
	#(CallTargetBrowser GraalInfoMorph
		PolyglotEditor PolyglotNotebook PolyglotWorkspace
		TruffleSqueakUtilities)
			do: [ :ea | Smalltalk at: ea ifPresent: [ :cls | cls registerInWorldMenu] ].
	