message list
callTargetInfoContents
	^ contents := self selectedClassOrMetaClass methodDict
		at: self selectedMessageName
		ifPresent: [ :cm | self class callTargetInfoStringFor: cm ]
		ifAbsent: [ 'not found' ]
