testing
testJavaDuration
	| zero nonZero |
	self deny: (Interop isDuration: 42).

	zero := (Java type: 'java.time.Duration') ZERO.
	self assert: (Interop isDuration: zero).
	self assert: zero asDuration isZero.
	
	nonZero := (Java type: 'java.time.Duration') ofSeconds: 123 _: 456.
	self assert: 123 equals: nonZero asDuration asSeconds.
	self assert: 456 equals: nonZero asDuration nanoSeconds.
	self assert: nonZero asDuration = (Duration seconds: 123 nanoSeconds: 456)