polyglot
codeForPolyglotEvaluateFile: path in: language

	^  'Polyglot.eval_file(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: path), ')'