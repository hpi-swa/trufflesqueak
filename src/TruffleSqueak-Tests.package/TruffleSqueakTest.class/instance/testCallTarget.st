testing
testCallTarget
	| method string |
	Java primitiveIsHostLookupAllowed ifFalse: [ ^ false ].
	
	method := SmallInteger >> #/.

	self deny: method callTarget isNil.

	string := CallTargetBrowser callTargetInfoStringFor: method.
	self assert: (string includesSubstring: 'knownCallSiteCount')