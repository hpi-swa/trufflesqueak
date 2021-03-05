*TruffleSqueak-Core-duration
interopAsDuration
	^ Interop asHostObject: ((Java type: 'java.time.Duration')
		ofSeconds: seconds nanoAdjustment: nanos)