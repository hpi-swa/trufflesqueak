Class {
	#name : #PETabbedPane,
	#superclass : #Morph,
	#instVars : [
		'editAreas',
		'tabs',
		'editArea'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #'edit areas' }
PETabbedPane >> addEditArea: aMorph [

	| eventMap |
	editAreas add: aMorph.
	
	eventMap := Dictionary newFrom: {
		#save -> #activeEditAreaContentChanged.
		#lineEndingsChanged -> #activeEditAreaContentChanged.
		#generatePolyglotCodeBox -> #activeEditAreaGeneratePolyglotCodeBox.
		#generatePolyglotEvaluate -> #activeEditAreaGeneratePolyglotEvaluate.
		#generatePolyglotImport -> #activeEditAreaGeneratePolyglotImport.
		#generatePolyglotExport -> #activeEditAreaGeneratePolyglotExport.
		#searchForCode -> #activeEditAreaSearchForCode}.
	
	eventMap keysAndValuesDo: [:incomingEvent :triggeredEvent | aMorph model
		when: incomingEvent 
		send: #triggerEvent:with: 
		to: self
		withArguments: {triggeredEvent. aMorph}].
	
	aMorph model when: #newSnippetDirectory send: #newSnippetDirectory: to: self.
	aMorph model when: #focus send: #makeEditAreaActive: to: self with: aMorph
]

{ #category : #public }
PETabbedPane >> frameFractions: aRectangle [

	| frame |
	frame := self layoutFrame ifNil: [LayoutFrame new].
	frame
		topFraction: aRectangle top;
		bottomFraction: aRectangle bottom;
		leftFraction: aRectangle left;
		rightFraction: aRectangle right.
	self layoutFrame: frame.
	self layoutChanged
]

{ #category : #'events-processing' }
PETabbedPane >> handleKeystroke: anEvent [ 

	(anEvent commandKeyPressed and: [anEvent keyCharacter = $w]) ifTrue: [
			self flag: #todo "close current tab and edit area"].
	
	super handleKeystroke: anEvent
]

{ #category : #initialization }
PETabbedPane >> initialize [

	super initialize.
	
	editArea := nil.
	editAreas := OrderedCollection new.
	self
		changeTableLayout;
		listDirection: #topToBottom;
		hResizing: #spaceFill;
		vResizing: #spaceFill;
		color: Color transparent;
		addMorphBack: (tabs := PETabsMorph new)
]

{ #category : #public }
PETabbedPane >> isVisibleEditAreaActive [

	self visibleEditArea ifNil: [^ false].
	^ tabs hasActiveTab
]

{ #category : #'edit areas' }
PETabbedPane >> makeEditAreaActive: aMorph [

	tabs setTabActive: (editAreas indexOf: aMorph).
	editArea ~= aMorph ifTrue: [
		editArea ifNotNil: [editArea delete].
		editArea := aMorph.
		self addMorphBack: aMorph	].
	self triggerEvent: #focused with: self.
	self triggerEvent: #activeEditAreaContentChanged with: aMorph
]

{ #category : #'events-processing' }
PETabbedPane >> newSnippetDirectory: aPCFileEntryDirectory [

	self triggerEvent: #newSnippetDirectory with: aPCFileEntryDirectory
]

{ #category : #public }
PETabbedPane >> openFile: aPCFileEntry [

	editAreas detect: [:each | each model fileEntry = aPCFileEntry] 
		ifFound: [:existingEditArea | self makeEditAreaActive: existingEditArea]
		ifNone: [ | editAreaClass newEditArea |
			editAreaClass := PEEditAreaAbstract allSubclasses detectMax: [:each | 
				each supports: aPCFileEntry].
	
			newEditArea := ToolBuilder build: editAreaClass.
			newEditArea model openFile: aPCFileEntry.

			(tabs addTab: aPCFileEntry name)
				when: #clicked send: #makeEditAreaActive: to: self with: newEditArea; 
				when: #closed send: #removeEditArea: to: self with: newEditArea.
	
			self addEditArea: newEditArea.
			self makeEditAreaActive: newEditArea]
]

{ #category : #'edit areas' }
PETabbedPane >> removeEditArea: aMorph [

	| nextEditArea |
	nextEditArea := editAreas after: aMorph ifAbsent: [
		editAreas before: aMorph ifAbsent: [nil]].
	editAreas remove: aMorph.
	editArea = aMorph ifTrue: [
		editArea delete.
		editArea := nil.
		nextEditArea ifNotNil: [self makeEditAreaActive: nextEditArea]].
	editAreas ifEmpty: [self triggerEvent: #closed with: self]
]

{ #category : #public }
PETabbedPane >> setAllTabsToNonActive [

	tabs setAllTabsToNonActive
]

{ #category : #public }
PETabbedPane >> visibleEditArea [

	^ editArea
]
