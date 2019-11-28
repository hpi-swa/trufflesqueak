primitives
primitiveGetPolyglotBindings
	<primitive: 'primitiveGetPolyglotBindings' module: 'PolyglotPlugin'>
	self assert: self primitiveIsPolyglotBindingsAccessAllowed not description: 'Bindings access must not be allowed'.
	self error: 'Polyglot bindings access is not allowed'