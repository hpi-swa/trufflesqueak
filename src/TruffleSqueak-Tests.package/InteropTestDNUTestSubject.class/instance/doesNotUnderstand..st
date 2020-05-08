error handling
doesNotUnderstand: aMessage
	^ aMessage arguments
		ifEmpty: [ 1234 ]
		ifNotEmpty: [ :args | args size ]
	