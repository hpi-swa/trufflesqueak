Extension { #name : #DateAndTime }

{ #category : #'*TruffleSqueak-Core-date and time' }
DateAndTime >> interopAsDate [
	^ self asDate interopAsDate
]

{ #category : #'*TruffleSqueak-Core-date and time' }
DateAndTime >> interopAsInstant [
	^ Java asHostObject: ((Java type: 'java.time.Instant') ofEpochSecond: self asUnixTime)
]

{ #category : #'*TruffleSqueak-Core-date and time' }
DateAndTime >> interopAsTime [
	^ Java asHostObject: ((Java type: 'java.time.LocalTime')
		ofNanoOfDay: self asNanoSeconds)
]

{ #category : #'*TruffleSqueak-Core-date and time' }
DateAndTime >> interopAsTimeZone [
	^ self class localTimeZone interopAsTimeZone
]

{ #category : #'*TruffleSqueak-Core-date and time' }
DateAndTime >> interopIsDate [
	^ true
]

{ #category : #'*TruffleSqueak-Core-date and time' }
DateAndTime >> interopIsTime [
	^ true
]

{ #category : #'*TruffleSqueak-Core-date and time' }
DateAndTime >> interopIsTimeZone [
	^ true
]
