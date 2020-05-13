polyglot
codeForPolyglotExportOf: value to: name

	^ 'export(', (self codeForEscapedString: name), ', ', value, ')'