file operations
rename: aString

	self withFSDo: [:fs | fs rename: self treeEntry entryName to: aString]