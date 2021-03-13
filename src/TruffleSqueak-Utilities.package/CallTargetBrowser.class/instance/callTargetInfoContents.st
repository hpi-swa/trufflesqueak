message list
callTargetInfoContents
	^ contents := self selectedClassOrMetaClass methodDict
		at: self selectedMessageName
		ifPresent: [ :cm | self class callTargetInfoStringFor: cm callTarget ]
		ifAbsent: [ 'not found' ]
