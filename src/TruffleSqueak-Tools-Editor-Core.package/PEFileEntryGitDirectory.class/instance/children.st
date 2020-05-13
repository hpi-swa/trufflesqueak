accessing
children

	^ self treeEntry object entries values 
		"ignore submodules and symlinks"
		select: [:each | each isInDirectoryMode or: [each isInFileMode]]
		thenCollect: [:each | each isInDirectoryMode 
			ifTrue: [PEFileEntryGitDirectory on: each path: self path / each entryName] 
			ifFalse: [PEFileEntryGitFile on: each path: self path]]