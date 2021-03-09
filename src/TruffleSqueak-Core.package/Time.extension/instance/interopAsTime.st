*TruffleSqueak-Core-date and time
interopAsTime
	^ Java asHostObject: ((Java type: 'java.time.LocalTime')
		ofNanoOfDay: self asNanoSeconds)