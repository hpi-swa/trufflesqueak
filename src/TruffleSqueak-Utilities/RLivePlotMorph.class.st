Class {
	#name : #RLivePlotMorph,
	#superclass : #RPlotMorph,
	#instVars : [
		'collection'
	],
	#category : 'TruffleSqueak-Utilities'
}

{ #category : #examples }
RLivePlotMorph class >> example [
	"self example"
	^ (self on: ((0 to: 360 - 1)
				collect: [:x | (100.0 * x degreesToRadians sin) asInteger])) openInHand
]

{ #category : #'instance creation' }
RLivePlotMorph class >> on: aCollection with: rCode [
	^ self new on: aCollection with: rCode
]

{ #category : #initialization }
RLivePlotMorph >> initialize [
	super initialize.
	self extent: 600@400.
	self startStepping
]

{ #category : #initialization }
RLivePlotMorph >> on: aCollection with: rCode [
	| exportName |
	exportName := '_plot_data_', aCollection hash.
	Polyglot export: exportName value: aCollection.
	plotCode := '
	array <- import("{1}")@asArray()
	size <- array@size()
	values <- data.frame(x=1:size, y=array)
	print({2})'
		format: {exportName. rCode}.
]

{ #category : #'stepping and presenter' }
RLivePlotMorph >> step [
	self renderIntoForm.
	self changed
]

{ #category : #testing }
RLivePlotMorph >> stepTime [
	^ 2000
]
