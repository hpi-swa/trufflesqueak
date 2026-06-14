failures
expectedFailures
	^ Java isAOT
		ifTrue: [ #(testInstallLanguage) ]
		ifFalse: [ #() ]