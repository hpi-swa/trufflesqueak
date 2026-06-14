failures
expectedFailures
	^ Java isAOT
		ifTrue: [ #(testJavaAddToHostClassPath testLanguageClass) ]
		ifFalse: [ #() ]