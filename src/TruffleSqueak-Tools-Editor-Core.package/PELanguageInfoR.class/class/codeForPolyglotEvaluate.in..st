polyglot
codeForPolyglotEvaluate: code in: language

	^  'eval.polyglot(', (self codeForEscapedString: language), ', ', (self codeForEscapedString: code), ')'