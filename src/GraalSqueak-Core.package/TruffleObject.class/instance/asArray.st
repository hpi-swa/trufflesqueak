converting
asArray
	| size |
	size := self primitiveGetArraySize.
	Array new: size streamContents: [ :stream |
		1 to: size do: [ :index | stream nextPut: (self basicAt: index) ]]