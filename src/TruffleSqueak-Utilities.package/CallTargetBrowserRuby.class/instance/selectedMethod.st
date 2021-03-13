methods
selectedMethod
	^ self selectedModuleName
		ifNil: [ nil ]
		ifNotNil: [ :moduleName |
			self selectedMethodName
				ifNil: [ nil ]
				ifNotNil: [ :methodName | self class methodFor: methodName of: moduleName] ]