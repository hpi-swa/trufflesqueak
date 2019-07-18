actions
displayResult: anObject
	self removeAllMorphs.
	self
		vResizing: #rigid;
		layoutPolicy: ProportionalLayout new.
	result := anObject.
	anObject isString ifTrue: [ | morph |
		morph := anObject asString asText asMorph readOnly: true; yourself.
		self addMorph: morph fullFrame: (LayoutFrame fractions: (0@0 corner: 1@1)).
		self addCornerGrips.
		^ self ].
	anObject isMorph ifTrue: [
		self
			vResizing: #shrinkWrap;
			layoutPolicy: TableLayout new.
		self addMorph: (anObject addFlexShell hResizing: #spaceFill; yourself).
		^ self ].
	"Add support for images, ..."
	self addMorph: (PolyglotNotebook explorerMorphFor: anObject) fullFrame: (LayoutFrame fractions: (0@0 corner: 1@1)).
	self addCornerGrips.
	