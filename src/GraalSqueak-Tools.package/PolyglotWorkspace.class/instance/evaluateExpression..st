evaluation
evaluateExpression: selection
	^ Polyglot
		eval: self languageID
		string: selection asString
		withBindingsNamed: 'bindings'