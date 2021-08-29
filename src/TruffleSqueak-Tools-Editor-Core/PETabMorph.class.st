Class {
	#name : #PETabMorph,
	#superclass : #Morph,
	#instVars : [
		'state'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #'instance creation' }
PETabMorph class >> title: aString [

	^ self new
		title: aString;
		yourself
]

{ #category : #private }
PETabMorph >> click [

	self state: #active.
	self triggerEvent: #clicked
]

{ #category : #private }
PETabMorph >> close [

	self delete.
	self triggerEvent: #closed
]

{ #category : #initialization }
PETabMorph >> initialize [

	| closeMorph |
	super initialize.
	
	closeMorph := SystemWindow closeBoxImage asMorph
		eventHandler: (EventHandler new
			on: #mouseDown send: #close to: self);
		yourself.
	
	self
		changeTableLayout;
		height: 25;
		vResizing: #rigid;
		hResizing: #shrinkWrap;
		layoutInset: 3;
		cellInset: 3;
		listDirection: #leftToRight;
		listCentering: #center;
		addMorphBack: StringMorph new;
		addMorphBack: closeMorph;
		eventHandler: (EventHandler new
			on: #mouseDown send: #click to: self);
		state: #unselected
]

{ #category : #private }
PETabMorph >> state [

	^ state
]

{ #category : #public }
PETabMorph >> state: aSymbol [

	self assert: (#(unselected selected active) includes: aSymbol).
	state := aSymbol.
	state caseOf: {
		[#unselected] -> [self color: (Color gray: 0.91). self titleColor: Color black].
		[#selected] -> [self color: Color white. self titleColor: Color black].
		[#active] -> [self color: Color white. self titleColor: Color red]}
]

{ #category : #private }
PETabMorph >> title: aString [

	self firstSubmorph contents: aString
]

{ #category : #private }
PETabMorph >> titleColor: aColor [

	self firstSubmorph color: aColor
]
