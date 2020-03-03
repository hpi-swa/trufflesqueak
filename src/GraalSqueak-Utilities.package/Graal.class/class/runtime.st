accessing
runtime
	^ runtime ifNil: [
		runtime := [
			(Graal primitiveLookupGraalSymbol: 'com.oracle.truffle.api.Truffle') getRuntime]
				on: Error do: [nil]]