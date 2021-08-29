Class {
	#name : #RPlotMorph,
	#superclass : #JavaImageMorph,
	#instVars : [
		'plotCode'
	],
	#category : 'TruffleSqueak-Utilities'
}

{ #category : #examples }
RPlotMorph class >> example [
	"self example"
	(self extent: 400@300
		plotCode: 'print(ggplot(mpg, aes(displ, hwy, colour = class)) + geom_point())')
			openInHand
]

{ #category : #'instance creation' }
RPlotMorph class >> extent: extent plotCode: aString [
	^ self new
		extent: extent;
		plotCode: aString;
		yourself
]

{ #category : #accessing }
RPlotMorph >> bindings [
	^ Polyglot bindings
]

{ #category : #copying }
RPlotMorph >> copy [
	^ self class extent: self extent plotCode: plotCode
]

{ #category : #actions }
RPlotMorph >> exportSVG [
	| fileName dimensions |
	fileName := UIManager default saveFilenameRequest: 'Please select file name' translated initialAnswer: 'plot.svg'.
	dimensions := UIManager default request: 'Dimensions (in inches)' initialAnswer: '7x7'.
	(dimensions includes: $x) ifTrue: [ | split |
		split := dimensions splitBy: 'x'.
		self exportSVG: fileName width: split first asInteger height: split second asInteger ]
]

{ #category : #actions }
RPlotMorph >> exportSVG: aFilename width: width height: height [
	Polyglot eval: 'R' string: ('
	library(ggplot2)
	library(lattice)
	data("mpg")
	bindings <- import("bindings")
	svg(filename = "{1}", width = {2}, height = {3})
	{4}
	dev.off()
	' format: { aFilename. width. height. plotCode }).
	self inform: aFilename, ' exported successfully!'
]

{ #category : #initialization }
RPlotMorph >> form [
	form ifNil: [ super form. self renderIntoForm ].
	^ form
]

{ #category : #accessing }
RPlotMorph >> plotCode: aString [
	plotCode := aString
]

{ #category : #drawing }
RPlotMorph >> renderIntoForm [
	Polyglot eval: Polyglot languageIdR string: ('
library(ggplot2)
library(lattice)
data("mpg")
awt(extent$x, extent$y, g2d)
{1}' format: { plotCode })
		names: #('bindings' 'extent' 'g2d')
		arguments: {self bindings. self extent. self graphics2D}
]

{ #category : #'event handling' }
RPlotMorph >> yellowButtonActivity: shiftState [
	MenuMorph new
		add: 'export SVG...' target: self selector: #exportSVG;
		add: 'copy...' target: [ self copy openInHand ] selector: #value;
		add: 'clone form...' target: [ self form asMorph openInHand ] selector: #value;
		add: 'inspect...' target: self selector: #inspect;
		popUpInWorld
]
