polyglot
codeForPolyglotEvaluate: code in: language

	^  'polyglot.eval(language=', (self codeForEscapedString: language), ', string=', (self codeForEscapedString: code), ')'