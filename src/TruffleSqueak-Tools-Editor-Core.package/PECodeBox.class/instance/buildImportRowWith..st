building
buildImportRowWith: aBuilder

	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		verticalResizing: #shrinkWrap;
		horizontalResizing: #spaceFill;
		minimumHeight: TextStyle defaultFont height + 2;
		children: {
			PEPluggableLabelSpec new
				label: 'Imports: ';
				yourself.
			aBuilder pluggableInputFieldSpec new
				model: self;
				setText: #importVariablesFromInput:;
				getText: #importVariablesForInput;
				editText: #importVariablesFromInput:;
				indicateUnacceptedChanges: false;
				yourself.
			aBuilder pluggableButtonSpec new
				model: self;
				label: 'Code Box';
				action: #nestedCodeBox;
				verticalResizing: #spaceFill;
				horizontalResizing: #rigid;
				yourself};
		yourself