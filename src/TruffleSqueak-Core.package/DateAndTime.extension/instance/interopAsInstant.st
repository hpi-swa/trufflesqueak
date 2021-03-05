*TruffleSqueak-Core-date and time
interopAsInstant
	^ Interop asHostObject: ((Java type: 'java.time.Instant') ofEpochSecond: self asUnixTime)