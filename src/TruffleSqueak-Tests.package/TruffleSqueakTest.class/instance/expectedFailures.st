failures
expectedFailures
	^ Polyglot isSupportedByVM
		ifTrue: [ #() ]
		ifFalse: [ self class allTestSelectors ]