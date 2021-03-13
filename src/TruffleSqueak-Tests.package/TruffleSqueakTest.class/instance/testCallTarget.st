testing
testCallTarget
	| method string |
	Java primitiveIsHostLookupAllowed ifFalse: [ ^ false ].
	
	self assert: (SmalltalkImage >> #exitToDebugger) callTarget isNil.
	
	method := Integer >> #benchFib.
	1 benchFib. "Ensure method is called"

	self deny: method callTarget isNil.

	string := CallTargetBrowser callTargetInfoStringFor: method callTarget.
	self assert: (string includesSubstring: 'knownCallSiteCount')
