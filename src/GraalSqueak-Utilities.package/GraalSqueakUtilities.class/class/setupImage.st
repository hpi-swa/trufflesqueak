image creation
setupImage
	"Install polyglot tools"
	PolyglotToolSet register.

	"Avoid UnknownSelector popups"
	UnknownSelector compile: 'openMenuIn: aBlock
	^ self resume: name asSymbol'.

	"Disable performance killers"
	Morph useSoftDropShadow: false.
	SystemWindow gradientWindow: false.
	DialogWindow gradientDialog: false.
	MenuMorph gradientMenu: false.
	PluggableButtonMorph gradientButton: false.
	ScrollBar gradientScrollBar: false.

	Morph indicateKeyboardFocus: false.
	
	"Set up environment"
	ReleaseBuilder deleteAllWindows.
	self openButton: PolyglotWorkspace asString, ' open' at: 20@28.
	self openButton: PolyglotNotebook asString, ' open' at: 190@28.

	self logo asMorph
		position: 650@28;
		setToAdhereToEdge: #bottomRight;
		beSticky;
		openInWorld.

	PolyglotWorkspace open
		position: 20@60;
		extent: 600@300.

	"Clean up image"
	Smalltalk garbageCollect.
	Smalltalk condenseChanges.