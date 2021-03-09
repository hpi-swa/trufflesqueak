*TruffleSqueak-Core-date and time
interopAsTimeZone
	| zoneId |
	zoneId := (Java type: 'java.time.ZoneId').
	^ Java asHostObject: (
		self abbreviation = 'LT'
			ifTrue: [ zoneId systemDefault ]
			ifFalse: [ zoneId of: self abbreviation ])