editor functions
setNewRootDirectory: aDirectory

	self rootDirectories: {(PEFileEntryFSDirectory on: aDirectory)}, self gitDirectories