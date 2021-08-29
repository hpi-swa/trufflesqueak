Class {
	#name : #PEPluggablePanelSpec,
	#superclass : #PluggablePanelSpec,
	#instVars : [
		'onClick'
	],
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #building }
PEPluggablePanelSpec >> buildWith: aBuilder [

	^ aBuilder buildPCPluggablePanel: self
]

{ #category : #accessing }
PEPluggablePanelSpec >> onClick [

	^ onClick
]

{ #category : #accessing }
PEPluggablePanelSpec >> onClick: aSymbol [

	onClick := aSymbol
]
