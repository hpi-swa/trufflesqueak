Class {
	#name : #PEPluggableLabelSpec,
	#superclass : #PluggableWidgetSpec,
	#instVars : [
		'label'
	],
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #building }
PEPluggableLabelSpec >> buildWith: aBuilder [

	^ aBuilder buildPluggableLabel: self
]

{ #category : #accessing }
PEPluggableLabelSpec >> label [

	^ label
]

{ #category : #accessing }
PEPluggableLabelSpec >> label: aSymbol [
	"Indicate the selector for retrieving the label"
	label := aSymbol.
]
