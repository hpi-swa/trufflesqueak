testing
testCallTarget
	| method string |
	Java primitiveIsHostLookupAllowed ifFalse: [ ^ false ].
	
	self assert: (SmalltalkImage >> #exitToDebugger) callTarget isNil.
	
	method := SmallInteger >> #/.
	4/2. "Ensure method is called"

	self deny: method callTarget isNil.

	string := CallTargetBrowser callTargetInfoStringFor: method.
	self assert: (string includesSubstring: 'knownCallSiteCount')