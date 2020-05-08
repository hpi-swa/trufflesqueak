testing
testJavaException
	| ex |
	self deny: (Interop isException: 42).

	ex := (Java type: 'java.lang.RuntimeException') new.
	self assert: (Interop isException: ex).