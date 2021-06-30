nbformat
offerToValidateOutput: aJsonString
	(self confirm: 'Would you like to validate the output with nbformat?') ifTrue: [
		(self isValidNBJson: aJsonString)
			ifTrue: [ self inform: 'Validation with nbformat succeeded!' ]
			ifFalse: [ self error: 'Validation with nbformat failed!' ] ]