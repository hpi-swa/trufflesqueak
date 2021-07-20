image creation
setUpAfterLoadingPackages
	"Called by LoadTruffleSqueakPackages.st as part of SqueakSUnitTest"
	SystemBrowser default: Browser.
	ToolSet default: PolyglotToolSet.