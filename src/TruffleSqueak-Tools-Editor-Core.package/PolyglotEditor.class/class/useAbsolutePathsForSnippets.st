preferences
useAbsolutePathsForSnippets

	<preference: 'Use absolute paths for snippets'
		category: 'Polyglot Editor'
		description: 'When on, snippets will use absolute paths'
		type: #Boolean>
	^ AbsolutePathsForSnippets ifNil: [false]