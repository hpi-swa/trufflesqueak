accessing
runtime
	^ (Graal primitiveLookupGraalSymbol: 'com.oracle.truffle.api.Truffle') getRuntime