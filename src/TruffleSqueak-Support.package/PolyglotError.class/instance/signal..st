signaling
signal: anErrorObject
	errorObject := anErrorObject.
	self messageText: anErrorObject asString.
	^ self signal