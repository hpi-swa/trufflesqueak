file operations
createDirectory: aString

	(self fileDirectory directoryExists: aString) ifFalse: [
		self fileDirectory createDirectory: aString].
	^ PEFileEntryFSDirectory on: (self fileDirectory directoryNamed: aString)