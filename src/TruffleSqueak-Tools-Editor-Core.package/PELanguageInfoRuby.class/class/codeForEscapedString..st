polyglot
codeForEscapedString: string

	string lineCount > 1 ifTrue: [UIManager inform: 'Cannot properly escape multiline string for JavaScript'].
	^ '"', (string copyReplaceAll: '"' with: '\"'),  '"'