code generation
polyglotCodeBox

	| codeBox snippetFile snippetFiles |
	snippetFiles := (snippetDirectories collect: #children) asArray flatten.
	self activeEditArea fileEntry parent children
		detect: [:each | each name = 'snippets']
		ifFound: [:snippets |
			snippetFile := UIManager default 
				chooseFrom: {'new'}, (snippetFiles collect: #name)
				values: {#new}, snippetFiles.
			snippetFile ifNil: [^ self].
			codeBox := snippetFile == #new
				ifTrue: [PECodeBox new]
				ifFalse: [(PECodeBoxReadWriter codeBoxForSnippet: snippetFile) first]]
		ifNone: [codeBox := PECodeBox new].
	self insertCodeBox: codeBox