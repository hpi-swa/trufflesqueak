polyglot
codeForPolyglotEvaluateFile: path in: language

	^  'eval.polyglot(', (self codeForEscapedString: language), ', path=', (self codeForEscapedString: path), ')'