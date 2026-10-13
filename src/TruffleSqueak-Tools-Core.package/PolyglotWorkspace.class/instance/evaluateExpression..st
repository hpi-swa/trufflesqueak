evaluation
evaluateExpression: selection
	^ Polyglot evalWithBindings: self languageInfo getId string: selection asString
