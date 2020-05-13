instance creation
on: aGitTreeEntry path: aFSPath

	self assert: [aGitTreeEntry isInDirectoryMode].
	^ self new
		treeEntry: aGitTreeEntry;
		path: aFSPath;
		yourself