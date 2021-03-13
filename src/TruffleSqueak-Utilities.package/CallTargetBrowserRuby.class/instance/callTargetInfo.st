call targets
callTargetInfo
	^ self selectedCallTarget
		ifNotNil: [ :c | CallTargetBrowser callTargetInfoStringFor: c ]
		ifNil: [ 'no call target available' ]