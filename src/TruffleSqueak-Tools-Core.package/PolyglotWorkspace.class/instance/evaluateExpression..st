evaluation
evaluateExpression: selection
	^ Polyglot
		eval: self languageInfo getId asString
		string: selection asString
		withBindingsNamed: 'bindings'