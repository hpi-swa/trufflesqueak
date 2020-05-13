polyglot
codeForPolyglotExportOf: value to: name

	^ 'polyglot.export_value(', value, ', ', (self codeForEscapedString: name), ')'