polyglot
codeForPolyglotImport: name

	^ 'Polyglot.import(', (self codeForEscapedString: name), ')'