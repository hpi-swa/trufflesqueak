testing
testJavaInstant
	| now |
	self deny: (Interop isInstant: 42).

	now := (Java type: 'java.time.Instant') now.
	self assert: (Interop isInstant: now).
	self assert: (now asDateAndTime asSeconds - DateAndTime current asSeconds) abs < 2.