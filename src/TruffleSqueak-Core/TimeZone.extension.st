Extension { #name : #TimeZone }

{ #category : #'*TruffleSqueak-Core-date and time' }
TimeZone >> interopAsTimeZone [
	| zoneId |
	zoneId := (Java type: 'java.time.ZoneId').
	^ Java asHostObject: (
		self abbreviation = 'LT'
			ifTrue: [ zoneId systemDefault ]
			ifFalse: [ zoneId of: self abbreviation ])
]

{ #category : #'*TruffleSqueak-Core-date and time' }
TimeZone >> interopIsTimeZone [
	^ true
]
