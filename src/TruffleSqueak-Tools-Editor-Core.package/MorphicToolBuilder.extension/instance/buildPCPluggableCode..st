*TruffleSqueak-Tools-Editor-Core
buildPCPluggableCode: aSpec
	| widget |
	"Largely based on buildPluggableText:"
	widget := PEPluggableCodeMorph basicNew.
	aSpec onKeyStroke ifNotNil: [widget setOnKeyStrokeSelector: aSpec onKeyStroke].
	widget initialize.
	
	aSpec stylerClass ifNotNil: [:c | widget styler: (c new view: widget)].
	aSpec styler ifNotNil: [:styler | widget styler: (styler view: widget)].
	
	aSpec onFocus ifNotNil: [widget setOnFocusSelector: aSpec onFocus].
	
	widget setLineEndingsSelector: aSpec setLineEndings.
	
	widget
		on: aSpec model
		text: aSpec getText 
		accept: aSpec setText
		readSelection: aSpec selection 
		menu: aSpec menu.
	aSpec font ifNotNil: [:f | widget font: f].
	
	widget setSelectionSelector: aSpec setSelection.
	widget setEditTextSelector: aSpec setEditText.
	
	widget readOnly: aSpec readOnly.
	widget editTextSelector: aSpec editText.
	widget wantsFrameAdornments: aSpec indicateUnacceptedChanges.

	widget askBeforeDiscardingEdits: aSpec askBeforeDiscardingEdits.
	widget setProperty: #alwaysAccept toValue: aSpec askBeforeDiscardingEdits not.

	aSpec softLineWrap ifNotNil: [:b | widget wrapFlag: b].

	widget isAutoFit
		ifTrue: [widget hideHScrollBarIndefinitely]
		ifFalse: [widget showHScrollBarOnlyWhenNeeded].
	
	self register: widget id: aSpec name.
	widget getColorSelector: aSpec color.
	widget getTextColorSelector: aSpec textColor.
	self buildHelpFor: widget spec: aSpec.
	 
	self setFrame: aSpec frame in: widget.
	self setLayoutHintsFor: widget spec: aSpec.
	
	aSpec autoHeight ifTrue: [
		widget autoHeight: true.
		widget vResizing: #shrinkWrap.
		"widget vScrollBarPolicy: #never"].
	
	parent ifNotNil:[self add: widget to: parent].
	
	^widget