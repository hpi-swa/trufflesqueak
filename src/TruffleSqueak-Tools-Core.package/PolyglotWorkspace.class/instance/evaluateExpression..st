evaluation
evaluateExpression: selection
	^ Polyglot isPolyglotEvalAllowed
		ifTrue: [ Polyglot
					eval: self languageInfo getId asString
					string: selection asString
					withBindingsNamed: 'bindings' ]
		ifFalse: [ Compiler evaluate: selection ]