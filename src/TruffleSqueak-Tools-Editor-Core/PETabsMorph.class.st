Class {
	#name : #PETabsMorph,
	#superclass : #Morph,
	#instVars : [
		'tabHolder',
		'scrollPane'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #public }
PETabsMorph >> addTab: aString [

	| tab |
	tab := PETabMorph title: aString.
	tab
		when: #closed send: #updateScrollbars to: self;
		when: #clicked send: #tabClicked: to: self with: tab.
	tabHolder addMorphBack: tab.
	self updateScrollbars.
	^ tab
]

{ #category : #public }
PETabsMorph >> hasActiveTab [

	^ self tabs detect: [:each | each state = #active] 
		ifFound: [:tab | true]
		ifNone: [false]
]

{ #category : #initialization }
PETabsMorph >> initialize [

	super initialize.
	self 
		changeTableLayout;
		hResizing: #spaceFill;
		vResizing: #shrinkWrap;
		color: Color transparent.
	
	tabHolder := Morph new
		color: Color transparent;
		changeTableLayout;
		listDirection: #leftToRight;
		hResizing: #shrinkWrap;
		vResizing: #shrinkWrap;
		cellInset: 3;
		yourself.
	scrollPane := ScrollPane new.
	scrollPane
		color: Color transparent;
		hResizing: #spaceFill;
		height: 25 + scrollPane scrollBarThickness;
		vScrollBarPolicy: #never;
		hScrollBarPolicy: #always.
	scrollPane scroller addMorphBack: tabHolder.
	self addMorph: scrollPane
]

{ #category : #public }
PETabsMorph >> setAllTabsToNonActive [

	self tabs do: [:each | each state = #active ifTrue: [each state: #selected]]
]

{ #category : #public }
PETabsMorph >> setTabActive: aNumber [

	self tabs withIndexDo: [:each :index |
		index = aNumber
			ifTrue: [
				each state: #active.
				scrollPane scrollToShow: (each position x@0 extent: each width@0)] 
			ifFalse: [each state: #unselected]]
]

{ #category : #public }
PETabsMorph >> tabClicked: aPCTabMorph [

	self tabs do: [:each | each state: #unselected] without: aPCTabMorph
]

{ #category : #private }
PETabsMorph >> tabHeight [

	^ 25
]

{ #category : #private }
PETabsMorph >> tabs [

	^ tabHolder submorphs
]

{ #category : #private }
PETabsMorph >> updateScrollbars [

	scrollPane
		resizeScrollBars;
		resizeScroller;
		setScrollDeltas;
		hideOrShowScrollBars
]
