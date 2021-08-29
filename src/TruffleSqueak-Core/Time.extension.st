Extension { #name : #Time }

{ #category : #'*TruffleSqueak-Core-date and time' }
Time >> interopAsTime [
	^ Java asHostObject: ((Java type: 'java.time.LocalTime')
		ofNanoOfDay: self asNanoSeconds)
]

{ #category : #'*TruffleSqueak-Core-date and time' }
Time >> interopIsTime [
	^ true
]
