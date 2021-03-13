call targets
selectedCallTarget
	^ self selectedModuleName
		ifNil: [ nil ]
		ifNotNil: [ :moduleName |
			self selectedMethodName
				ifNil: [ nil ]
				ifNotNil: [ :methodName | self class callTargetFor: methodName of: moduleName] ]