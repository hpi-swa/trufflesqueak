testing
testIdentityAPIs
	(Polyglot isPolyglotEvalAllowed) ifFalse: [ ^self ].

	"Boxed objects have an identity"
	{nil. 'foo'. #('bar'). Object new. Smalltalk} do: [ :ea |
		self assert: (Interop hasIdentity: ea).
		self assert: (Interop isIdentical: ea to: ea).
		self assert: (Interop identityHashCode: ea) equals: (Interop identityHashCode: ea) ].

	"Unboxed objects do not have an identity"
	#(true false 42 12.34 $x) do: [ :ea |
		self deny: (Interop hasIdentity: ea).
		self deny: (Interop isIdentical: ea to: ea) ]
