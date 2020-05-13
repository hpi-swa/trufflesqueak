instance creation
forPath: aString

	| path |
	path := aString copyReplaceAll: '\' with: '/'.
	^ PEFileEntryTestFile allInstances detect: [:each | each fullPath = path]