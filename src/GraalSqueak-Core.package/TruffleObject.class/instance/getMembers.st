interop
getMembers
	| members |
	members := self primitiveGetMembers.
	^ Array new: members size streamContents: [ :stream |
		1 to: members size do: [ :index | stream nextPut: (members basicAt: index) asString ]]