snippet directories
findSnippetSubDirectoriesIn: aPCFileEntryDirectory upToLevel: aNumber

	| snippetDirectories |
	snippetDirectories := Set new.
	aPCFileEntryDirectory children 
		select: [:each | each isFile not]
		thenDo: [:each |
			each name = 'snippets'
				ifTrue: [snippetDirectories add: each]
				ifFalse: [aNumber > 0 ifTrue: [
					snippetDirectories addAll: (self findSnippetSubDirectoriesIn: each upToLevel: aNumber - 1)]]].
	^ snippetDirectories