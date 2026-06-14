failures
expectedFailures
	^ (Java primitiveIsHostLookupAllowed not or: [Java isAOT])
		ifTrue: [ #(testInstallLanguage) ]
		ifFalse: [ #() ]