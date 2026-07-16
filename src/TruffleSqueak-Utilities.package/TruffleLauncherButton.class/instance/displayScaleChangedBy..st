display scale
displayScaleChangedBy: factor
	"The system's scale factor has changed. Try to scale the receiver so that it looks nice on the display. See DisplayScreen >> #uiScaleFactor:."
	
	"Font pointSizes do not scale linearly so we need to keep track of the exact pointSizes that are encountered."
	(self findA: StringMorph) ifNotNil: [ :label | | fonts oldFont oldPointSize newPointSize newFont unscaledPointSize |
		
		fonts := self valueOfProperty: #fontSizeTable ifAbsentPut: [ Dictionary new ].
		
		oldFont := label font.
		oldPointSize := oldFont pointSize.
		
		newPointSize := oldPointSize * factor.
		newFont := fonts at: newPointSize ifAbsentPut: [ oldFont asPointSize: newPointSize ].
		
		unscaledPointSize := newFont pointSize / factor.
		(fonts includesKey: unscaledPointSize) ifFalse: [ fonts at: unscaledPointSize put: oldFont ].
		
		label font: newFont.
	].

	super displayScaleChangedBy: factor.
