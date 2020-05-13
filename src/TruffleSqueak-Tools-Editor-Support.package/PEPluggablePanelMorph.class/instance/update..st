update
update: aSymbol

	aSymbol = getColorSelector ifTrue: [
		^ self color: (self model perform: getColorSelector)].
	aSymbol = getChildrenSelector ifTrue: [
		"Overwrite super implementation to call >>addMorph:fullFrame and >>removeMorphAndRestoreLayoutFrame: for Morphs with a LayoutFrame"
		self removePaneSplitters.
		self submorphsDo: [:each | each layoutFrame 
			ifNil: [self removeMorph: each]
			ifNotNil: [self removeMorphAndRestoreLayoutFrame: each]].
		self children do: [:each | each layoutFrame
			ifNil: [self addMorph: each]
			ifNotNil: [self addMorph: each fullFrame: each layoutFrame]].
			
		"Let my children take all the place unless my extent is described by them."
		self hResizing ~~ #shrinkWrap 
			ifTrue: [self submorphsDo: [: m | m hResizing: #spaceFill]].
		self vResizing ~~ #shrinkWrap
			ifTrue: [self submorphsDo: [: m | m vResizing: #spaceFill]].
		
		"Tell dependents about this update. There is no pluggable notification for changed submorphs."
		^ self changed: #submorphs].
	
	^ super update: aSymbol