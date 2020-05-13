polyglot
codeForPolyglotEvaluate: code in: language

	^  'Polyglot.eval(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: code), ')'