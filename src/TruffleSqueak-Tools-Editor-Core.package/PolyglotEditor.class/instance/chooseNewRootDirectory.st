editor functions
chooseNewRootDirectory

	| folder currentRoot |
	currentRoot := self rootDirectories detect: [:each | each isKindOf: PEFileEntryFSDirectory]
		ifFound: [:directory | directory fileDirectory]
		ifNone: [FileDirectory default].
	folder := UIManager default chooseDirectory: 'Choose a new root directory' from: currentRoot.
	folder ifNotNil: [self setNewRootDirectory: folder]