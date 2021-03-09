*TruffleSqueak-Core-duration
interopAsDuration
	^ Java asHostObject: ((Java type: 'java.time.Duration')
		ofSeconds: seconds nanoAdjustment: nanos)