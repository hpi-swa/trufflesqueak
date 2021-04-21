image creation
setUpImage
	"Install polyglot tools"
	PolyglotToolSet register.
	ToolSet default: PolyglotToolSet.
	
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
	
	"Set up environment"
	ReleaseBuilder deleteAllWindows.
	self openButton: PolyglotWorkspace asString, ' open' at: 20@28.
	self openButton: PolyglotNotebook asString, ' open' at: 190@28.

	self logo asMorph
		position: 650@28;
		borderWidth: 10;
		setToAdhereToEdge: #bottomRight;
		beSticky;
		openInWorld.

	PolyglotWorkspace open
		position: 20@60;
		extent: 600@300.

	"Clean up image"
	Smalltalk garbageCollect.
	Smalltalk condenseChanges.