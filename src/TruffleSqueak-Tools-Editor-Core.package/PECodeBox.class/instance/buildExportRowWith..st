building
buildExportRowWith: aBuilder

	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		verticalResizing: #shrinkWrap;
		horizontalResizing: #spaceFill;
		minimumHeight: TextStyle defaultFont height + 2;
		children: {
			PEPluggableLabelSpec new
				label: 'Export: ';
				yourself.
			aBuilder pluggableInputFieldSpec new
				model: self;
				setText: #exportVariablesFromInput:;
				getText: #exportVariablesForInput;
				editText: #exportVariablesFromInput:;
				indicateUnacceptedChanges: false;
				yourself};
		yourself