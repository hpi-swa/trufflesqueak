testing
expectedFailures
	^ Polyglot supportsMultipleLanguages
		ifTrue: [ #() ]
		ifFalse: [ self class allTestSelectors ]