Class {
	#name : #PEPluggablePanelMorph,
	#superclass : #PluggablePanelMorph,
	#instVars : [
		'getColorSelector',
		'onClickSelector'
	],
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #drawing }
PEPluggablePanelMorph >> drawOn: aCanvas [

	self perform: #drawOn: withArguments: {aCanvas} inSuperclass: RectangleMorph
]

{ #category : #accessing }
PEPluggablePanelMorph >> getColorSelector: aSymbol [

	getColorSelector := aSymbol
]

{ #category : #'event handling' }
PEPluggablePanelMorph >> handlesMouseDown: evt [

	onClickSelector ifNotNil: [^ true].
	^ super handlesMouseDown: evt
]

{ #category : #'event handling' }
PEPluggablePanelMorph >> mouseDown: evt [

	onClickSelector ifNil: [^ super mouseDown: evt].
	onClickSelector isSymbol 
		ifTrue: [self model perform: onClickSelector]
		ifFalse: [onClickSelector value]
]

{ #category : #accessing }
PEPluggablePanelMorph >> onClickSelector: aSymbol [

	onClickSelector := aSymbol
]

{ #category : #update }
PEPluggablePanelMorph >> removeMorphAndRestoreLayoutFrame: aMorph [ 

	"Same as addMorph:fullFrame, but inverts the spacing"
	self cellInset > 0 ifTrue: [ | left right bottom top spacing layoutFrame |
		layoutFrame := aMorph layoutFrame.

		spacing := self cellInset negated.
		left := layoutFrame leftOffset ifNil: [0].
		right := layoutFrame rightOffset ifNil: [0].

		bottom := layoutFrame bottomOffset ifNil: [0].
		top := layoutFrame topOffset ifNil: [0].
		
		"Add a spacing to the frame if it is not top or leftmost."
		layoutFrame leftFraction = 0
			ifFalse: [left := left + spacing]
			ifTrue: [
				"Expect another, resizable widget besides me if I have a fixed width."
				layoutFrame hasFixedWidth ifTrue: [right := right - spacing]].
		layoutFrame topFraction = 0
			ifFalse: [top := top + spacing]
			ifTrue: [
				"Expect another, resizable widget besides me if I have a fixed height."
				layoutFrame hasFixedHeight ifTrue: [bottom := bottom - spacing]].
			
		layoutFrame
			topOffset: top;
			bottomOffset: bottom;
			leftOffset: left;
			rightOffset: right].
		
	aMorph delete
]

{ #category : #update }
PEPluggablePanelMorph >> update: aSymbol [

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
]
