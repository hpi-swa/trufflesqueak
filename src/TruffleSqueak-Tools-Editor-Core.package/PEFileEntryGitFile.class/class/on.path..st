instance creation
on: aGitTreeEntry path: aFSPath

	self assert: [aGitTreeEntry isInFileMode].
	^ self new
		treeEntry: aGitTreeEntry;
		path: aFSPath
		yourself