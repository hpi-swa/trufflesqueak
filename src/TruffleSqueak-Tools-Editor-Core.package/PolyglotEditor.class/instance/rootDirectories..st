accessing
rootDirectories: aCollection

	rootDirectories := aCollection.
	snippetDirectories := self findSnippetSubDirectoriesUpToLevel: 3.
	self changed: #rootDirectories