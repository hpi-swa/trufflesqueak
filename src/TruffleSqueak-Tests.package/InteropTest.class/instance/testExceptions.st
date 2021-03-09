testing
testExceptions
	| e |
	
	e := Error new messageText: 'Error message'; yourself.
	
	self assert: (Interop isException: e).
	self assert: (Interop hasExceptionMessage: e).
	self assert: 'Error message' equals: (Interop getExceptionMessage: e).
	self deny: (Interop hasExceptionCause: e).
	self deny: (Interop hasExceptionStackTrace: e). "Not supported yet"

	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ].
	self assert: 'RUNTIME_ERROR' equals: (Interop getExceptionType: e).
	
	"JS tests disabled due to https://github.com/graalvm/graaljs/issues/373"
	true ifTrue: [ ^ self ].
	
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].
	
	e := Polyglot eval: 'js' string: 'try {foo()} catch (e) {e}'.
	self assert: (Interop isException: e).
	self assert: (Interop hasExceptionMessage: e).
	self assert: 'Error message' equals: (Interop getExceptionMessage: e).
	self assert: 'RUNTIME_ERROR' equals: (Interop getExceptionType: e).
	self assert: (Interop hasExceptionStackTrace: e).
	self deny: (Interop hasExceptionCause: e)
