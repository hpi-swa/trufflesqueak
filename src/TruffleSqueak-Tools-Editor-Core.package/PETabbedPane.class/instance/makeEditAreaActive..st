edit areas
makeEditAreaActive: aMorph

	tabs setTabActive: (editAreas indexOf: aMorph).
	editArea ~= aMorph ifTrue: [
		editArea ifNotNil: [editArea delete].
		editArea := aMorph.
		self addMorphBack: aMorph	].
	self triggerEvent: #focused with: self.
	self triggerEvent: #activeEditAreaContentChanged with: aMorph