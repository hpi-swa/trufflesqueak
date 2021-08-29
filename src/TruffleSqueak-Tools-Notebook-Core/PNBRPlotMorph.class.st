Class {
	#name : #PNBRPlotMorph,
	#superclass : #RPlotMorph,
	#instVars : [
		'bindings'
	],
	#category : #'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #accessing }
PNBRPlotMorph >> bindings [
	^ bindings
]

{ #category : #accessing }
PNBRPlotMorph >> bindings: anObject [
	bindings := anObject
]

{ #category : #'event handling' }
PNBRPlotMorph >> handlesMouseDown: anEvent [
	"Capture mouseDown events for right click menu in notebooks"
	^ true
]
