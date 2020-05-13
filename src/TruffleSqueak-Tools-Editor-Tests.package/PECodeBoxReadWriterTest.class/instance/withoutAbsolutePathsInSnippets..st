as yet unclassified
withoutAbsolutePathsInSnippets: aBlock

	| useAbsolutePathsForSnippets |
	useAbsolutePathsForSnippets := PolyglotEditor useAbsolutePathsForSnippets.
	PolyglotEditor useAbsolutePathsForSnippets: false.
	aBlock ensure: [PolyglotEditor useAbsolutePathsForSnippets: useAbsolutePathsForSnippets].