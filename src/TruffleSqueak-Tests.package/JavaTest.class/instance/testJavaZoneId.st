testing
testJavaZoneId
	| default |
	self deny: (Interop isTimeZone: 42).

	default := (Java type: 'java.time.ZoneId') systemDefault.
	self assert: (Interop isTimeZone: default).
	self assert: default asTimeZone abbreviation notEmpty.