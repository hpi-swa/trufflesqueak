instance creation
forPath: aString

	| directoryEntry |
	directoryEntry := FileDirectory
		splitName: aString
		to: [:dirPath :localName | (FileDirectory on: dirPath) directoryEntryFor: localName].
	^ self on: directoryEntry