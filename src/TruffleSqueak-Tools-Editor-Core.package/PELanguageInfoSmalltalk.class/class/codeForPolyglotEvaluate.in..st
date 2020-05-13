polyglot
codeForPolyglotEvaluate: code in: language

	^ 'Polyglot eval: ', (self codeForEscapedString: language), ' string: ', (self codeForEscapedString: code)