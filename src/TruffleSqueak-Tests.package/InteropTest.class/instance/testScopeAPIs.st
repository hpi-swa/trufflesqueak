testing
testScopeAPIs
	| s |
	(Polyglot isPolyglotEvalAllowed) ifFalse: [ ^self ].
	
	s := Polyglot primitiveGetScope: Polyglot languageIdSmalltalk.
	self assert: SmalltalkInteropScope instance equals: s.
	self assert: (Interop isScope: s).
	self deny: (Interop hasScopeParent: s).
	
	thisContext sender. "Ensure sender is available"
	s := thisContext.
	self assert: (Interop hasScopeParent: s).
	[ Interop hasScopeParent: s ] whileTrue: [
		s := Interop getScopeParent: s.
		self assert: (Interop isScope: s) ]