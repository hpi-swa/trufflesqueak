Class {
	#name : #PEFileExplorerMorph,
	#superclass : #PluggableTreeMorph,
	#instVars : [
		'realModel',
		'getRealRootsSelector',
		'setRealSelectedFileEntrySelector'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #accessing }
PEFileExplorerMorph >> getRealRootsSelector: aSymbol [

	getRealRootsSelector := aSymbol
]

{ #category : #accessing }
PEFileExplorerMorph >> realModel: anObject [

	realModel ifNotNil: [realModel removeDependent: self].
	anObject ifNotNil: [anObject addDependent: self].
	realModel := anObject.

]

{ #category : #accessing }
PEFileExplorerMorph >> setRealSelectedFileEntrySelector: aSymbol [

	setRealSelectedFileEntrySelector := aSymbol
]

{ #category : #selection }
PEFileExplorerMorph >> setSelectedMorph: aMorph [
	
	selectedWrapper := aMorph complexContents.
	realModel
		perform: setRealSelectedFileEntrySelector 
		with: (selectedWrapper ifNotNil: [:w | w item]).
	
	super setSelectedMorph: aMorph
]

{ #category : #updating }
PEFileExplorerMorph >> update: what [
	what ifNil:[^self].	
	what == getRootsSelector ifTrue: [
		self wrapRoots: (realModel perform: getRealRootsSelector).
		^ self].
	
	super update: what
]
