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

	"Clean up image"
	Smalltalk garbageCollect.
	Smalltalk condenseChanges.