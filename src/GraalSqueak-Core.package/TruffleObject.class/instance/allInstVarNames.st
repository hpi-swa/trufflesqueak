class behavior
allInstVarNames
	| keys |
	keys := self primitiveGetKeys.
	keys ifNil: [^ #()].
	^ keys reject: [:ea | (ea beginsWith: 'python_') or: [ ea = 'bindings' ] ]