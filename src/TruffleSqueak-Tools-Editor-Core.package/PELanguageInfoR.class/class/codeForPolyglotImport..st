polyglot
codeForPolyglotImport: name

	^ 'import(', (self codeForEscapedString: name), ')'