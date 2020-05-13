polyglot
codeForPolyglotEvaluateFile: path in: language

	^  'Polyglot.evalFile(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: path), ')'