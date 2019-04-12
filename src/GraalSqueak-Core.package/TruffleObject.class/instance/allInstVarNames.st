class behavior
allInstVarNames
	| members |
	members := self primitiveGetMembers.
	members ifNil: [^ #()].
	^ members