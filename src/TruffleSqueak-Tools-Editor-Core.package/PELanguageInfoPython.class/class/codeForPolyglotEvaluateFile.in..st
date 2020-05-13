polyglot
codeForPolyglotEvaluateFile: path in: language

	^  'polyglot.eval(language=', (self codeForEscapedString: language), ', path=', (self codeForEscapedString: path), ')'