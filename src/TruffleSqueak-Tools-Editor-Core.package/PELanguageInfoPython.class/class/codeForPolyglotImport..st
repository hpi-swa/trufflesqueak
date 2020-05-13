polyglot
codeForPolyglotImport: name

	^ 'polyglot.import_value(', (self codeForEscapedString: name), ')'