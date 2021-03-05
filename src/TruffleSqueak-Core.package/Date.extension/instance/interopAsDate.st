*TruffleSqueak-Core
interopAsDate
	^ Interop asHostObject: ((Java type: 'java.time.LocalDate')
		ofEpochDay: start julianDayNumber - start posixEpochJulianDays)