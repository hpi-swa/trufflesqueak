snippet directories
findSnippetSubDirectoriesUpToLevel: aNumber

	| snippetDirectories |
	snippetDirectories := Set new.
	self rootDirectories do: [:each |
		snippetDirectories addAll: (self findSnippetSubDirectoriesIn: each upToLevel: aNumber - 1)].
	^ snippetDirectories