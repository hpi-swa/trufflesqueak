testing
testJavaLocalTime
	| now |
	self deny: (Interop isTime: 42).

	now := (Java type: 'java.time.LocalTime') now.
	self assert: (Interop isTime: now).
	self assert: ((Date dateAndTimeNow second subtractTime: now asTime)) asDuration abs <  (Duration seconds: 2)