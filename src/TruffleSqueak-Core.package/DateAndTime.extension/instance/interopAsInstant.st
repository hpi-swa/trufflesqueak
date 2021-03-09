*TruffleSqueak-Core-date and time
interopAsInstant
	^ Java asHostObject: ((Java type: 'java.time.Instant') ofEpochSecond: self asUnixTime)