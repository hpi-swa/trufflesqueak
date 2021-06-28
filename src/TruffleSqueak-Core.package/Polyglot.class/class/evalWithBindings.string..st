experimental
evalWithBindings: aLanguage string: aString
	^ self eval: aLanguage string: aString names: #('bindings') arguments: {self bindings}