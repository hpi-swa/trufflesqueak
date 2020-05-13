polyglot
codeForEscapedString: string

	^ '"', (string copyReplaceAll: '"' with: '\"'),  '"'