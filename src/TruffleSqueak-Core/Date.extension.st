Extension { #name : #Date }

{ #category : #'*TruffleSqueak-Core' }
Date >> interopAsDate [
	^ Java asHostObject: ((Java type: 'java.time.LocalDate')
		ofEpochDay: start julianDayNumber - start posixEpochJulianDays)
]

{ #category : #'*TruffleSqueak-Core' }
Date >> interopIsDate [
	^ true
]
