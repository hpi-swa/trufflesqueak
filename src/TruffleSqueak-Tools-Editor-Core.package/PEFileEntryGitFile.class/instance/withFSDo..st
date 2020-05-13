private
withFSDo: aBlock

	| fs |
	fs := FileSystem onGit: self treeEntry repository workingDir.
	fs changeDirectory: (fs resolvePath: self path).
	aBlock value: fs.
	(UIManager default request: 'Commit Message') 
		ifNotEmpty: [:message | fs commit: message]