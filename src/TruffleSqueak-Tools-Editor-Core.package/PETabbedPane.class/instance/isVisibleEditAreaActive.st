public
isVisibleEditAreaActive

	self visibleEditArea ifNil: [^ false].
	^ tabs hasActiveTab