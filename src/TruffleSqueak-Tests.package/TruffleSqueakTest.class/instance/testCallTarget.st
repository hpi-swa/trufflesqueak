testing
testCallTarget
	| method callTarget string |
	
	self assert: (SmalltalkImage >> #exitToDebugger) callTarget isNil.
	
	method := Integer >> #benchFib.
	1 benchFib. "Ensure method is called"
	callTarget := method callTarget.

	self deny: callTarget isNil.
	
	(callTarget getClass getName asString includesSubstring: 'OptimizedCallTarget')
		ifFalse: [ ^ self ].

	string := CallTargetBrowser callTargetInfoStringFor: callTarget.
	self assert: (string includesSubstring: 'knownCallSiteCount')
