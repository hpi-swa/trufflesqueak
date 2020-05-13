polyglot
codeForEscapedString: string

	^ string lineCount > 1
		ifTrue: ['"""', (string copyReplaceAll: '"""' with: '\"""'),  '"""']
		ifFalse: ['"', ((string copyReplaceAll: '\' with: '\\') copyReplaceAll: '"' with: '\"'),  '"']