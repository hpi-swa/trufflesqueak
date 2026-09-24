failures
expectedFailures
	^ Java primitiveIsHostLookupAllowed ifTrue: [
			Java isAOT
				ifTrue: [ #(testJavaAddToHostClassPath) ]
				ifFalse: [ #() ]
		] ifFalse: [ self class allTestSelectors ]