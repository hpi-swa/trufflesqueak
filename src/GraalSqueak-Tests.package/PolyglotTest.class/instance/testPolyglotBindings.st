testing
testPolyglotBindings
	self shouldnt: [ Polyglot bindings at: 'bindings' ] raise: Error.
	self should: [ Polyglot bindings at: 'SmalltalkDict' ] raise: Error.
	
	self assert: nil equals: (Polyglot import: 'SmalltalkDict').
	self assert: Smalltalk equals: (Polyglot export: 'SmalltalkDict' value: Smalltalk).
	self assert: Smalltalk equals: (Polyglot import: 'SmalltalkDict').
	self assert: Smalltalk equals: (Polyglot bindings at: 'SmalltalkDict').
	
	Polyglot bindings primitiveRemoveMember: 'SmalltalkDict'.
	self should: [ Polyglot bindings at: 'SmalltalkDict' ] raise: Error.