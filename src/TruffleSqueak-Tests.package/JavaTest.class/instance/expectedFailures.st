failures
expectedFailures
	^ Java primitiveIsHostLookupAllowed
		ifTrue: [ #() ]
		ifFalse: [ self class allTestSelectors ]