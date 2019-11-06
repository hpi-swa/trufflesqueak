testing
testJavaAddToHostClassPath
	self assert: Java equals: (Java addToClassPath: 'some.jar').
	"No way to check #addToClassPath: succeeded (apart from looking up symbol that lives in some.jar)."