failures
expectedFailures
	^ Java primitiveIsHostLookupAllowed ifTrue: [
			Java isAOT
				ifTrue: [ #(testJavaAddToHostClassPath testLanguageClass) ]
				ifFalse: [ #() ]
		] ifFalse: [ self class allTestSelectors ]