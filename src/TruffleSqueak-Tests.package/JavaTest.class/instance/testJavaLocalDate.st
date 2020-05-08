testing
testJavaLocalDate
	| now |
	self deny: (Interop isDate: 42).

	now := (Java type: 'java.time.LocalDate') now.
	self assert: (Interop isDate: now).
	self assert: now asDate = Date dateAndTimeNow first