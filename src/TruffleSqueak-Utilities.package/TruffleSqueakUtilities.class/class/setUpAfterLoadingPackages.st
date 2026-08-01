image creation
setUpAfterLoadingPackages
	"Called by runSqueakTests.st"

	SystemBrowser default: Browser.
	ToolSet default: PolyglotToolSet.
	
	self patchBehaviorBasicRemoveSelector.