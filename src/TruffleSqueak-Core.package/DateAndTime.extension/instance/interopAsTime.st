*TruffleSqueak-Core-date and time
interopAsTime
	^ Interop asHostObject: ((Java type: 'java.time.LocalTime')
		ofNanoOfDay: self asNanoSeconds)