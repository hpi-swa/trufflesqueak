testing
testJavaAddToHostClassPath
	self assert: Java equals: (Java addToHostClassPath: 'some.jar').
	"No way to check #addToClassPath: succeeded (apart from looking up symbol that lives in some.jar)."