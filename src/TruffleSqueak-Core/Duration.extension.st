Extension { #name : #Duration }

{ #category : #'*TruffleSqueak-Core-duration' }
Duration >> interopAsDuration [
	^ Java asHostObject: ((Java type: 'java.time.Duration')
		ofSeconds: seconds nanoAdjustment: nanos)
]

{ #category : #'*TruffleSqueak-Core-duration' }
Duration >> interopIsDuration [
	^ true
]
