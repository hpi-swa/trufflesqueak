image creation
setUpImage
	"Install polyglot tools"
	| currentScale |
	
	PolyglotToolSet register.
	ToolSet default: PolyglotToolSet.
	
	"Set a default language."
	Polyglot askForDefaultLanguageInfo.
	
	"Ensure FullBlockClosure is in special objects array"
	(Smalltalk specialObjectsArray at: 38) ifNil:
		[ Smalltalk specialObjectsArray at: 38 put: FullBlockClosure ].

	"Avoid UnknownSelector popups"
	UnknownSelector compile: 'openMenuIn: aBlock
	^ self resume: name asSymbol "Patched by TruffleSqueak"'.

	"Disable performance killers"
	Morph useSoftDropShadow: false.
	SystemWindow gradientWindow: false.
	DialogWindow gradientDialog: false.
	MenuMorph gradientMenu: false.
	PluggableButtonMorph gradientButton: false.
	ScrollBar gradientScrollBar: false.

	Morph indicateKeyboardFocus: false.
	Project current flapsSuppressed: true.
	
	"Adjust main docking bar"
	TheWorldMainDockingBar
		showProjectsMenuInWorldMainDockingBar: false;
		showToolsMenuInWorldMainDockingBar: false.
	self adjustAppsMenu.
	self adjustDoMenu.
	TheWorldMainDockingBar compile: 'projectNameOn: aDockingBar
	^ self "Patched by TruffleSqueak"'.
	Project current showWorldMainDockingBar: false.
	Project current showWorldMainDockingBar: true.
	
	self closeAllWindowsAndMorphs.

	currentScale := Display platformScaleFactor.	

	self openButton: PolyglotWorkspace asString, ' open' at: 20@28 * currentScale.
	self openButton: PolyglotNotebook asString, ' open' at: 190@28 * currentScale.

	self logo asMorph
		position: 650@28 * currentScale;
		borderWidth: 10;
		setToAdhereToEdge: #bottomRight;
		beSticky;
		openInWorld.

	PolyglotWorkspace open
		position: 20@60 * currentScale;
		extent: 600@300 * currentScale.

	"Clean up image"
	Smalltalk garbageCollect.
	(self confirm: 'Condense changes?') ifTrue: [ Smalltalk condenseChanges ]
