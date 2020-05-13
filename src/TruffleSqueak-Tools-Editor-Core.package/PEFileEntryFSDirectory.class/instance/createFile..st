file operations
createFile: aString

	self fileDirectory fileNamed: aString.
	^ PEFileEntryFSFile on: (self fileDirectory directoryEntryFor: aString)