*TruffleSqueak-Core
interopAsDate
	^ Java asHostObject: ((Java type: 'java.time.LocalDate')
		ofEpochDay: start julianDayNumber - start posixEpochJulianDays)