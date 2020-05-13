polyglot
codeForPolyglotExportOf: value to: name

	^ 'Polyglot.export(', (self codeForEscapedString: name), ', ', value, ')'