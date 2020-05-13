polyglot
codeForPolyglotEvaluateFile: path in: language

	^ 'Polyglot eval: ', (self codeForEscapedString: language), ' file: ', (self codeForEscapedString: path)