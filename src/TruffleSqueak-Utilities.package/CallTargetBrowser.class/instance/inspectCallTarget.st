message functions
inspectCallTarget
	(self selectedClassOrMetaClass
		compiledMethodAt: self selectedMessageName
		ifAbsent: []) ifNotNil:
			[:method| method callTarget inspect]