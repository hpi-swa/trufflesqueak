baseline
baseline: spec
	<baseline>

	spec for: #'common' do: [ | needsJSON |
		"Hack: depend on JSON only if not present to avoid updates (quite slow on TruffleSqueak)"
		needsJSON := (Smalltalk at: #Json ifAbsent: [nil]) isNil.
		spec
			package: 'TruffleSqueak-Core';
			package: 'TruffleSqueak-Support' with: [spec requires: #('TruffleSqueak-Core')];
			package: 'TruffleSqueak-Tests' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Support' 'TruffleSqueak-Utilities')];
			package: 'TruffleSqueak-Tools-Core' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Tools-Support')];
			package: 'TruffleSqueak-Tools-Notebook-Core' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Utilities'),
				(needsJSON ifTrue: [#('JSON')] ifFalse: [#()])];
			package: 'TruffleSqueak-Tools-Support' with: [spec requires: #('TruffleSqueak-Core')];
			package: 'TruffleSqueak-Utilities' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Support')].
		needsJSON ifTrue: [
			spec package: 'JSON' with: [spec repository: 'http://www.squeaksource.com/JSON']].
		spec
			group: 'default' with: #('TruffleSqueak-Core' 'TruffleSqueak-Support' 'TruffleSqueak-Tools-Core' 'TruffleSqueak-Tools-Notebook-Core' 'TruffleSqueak-Utilities');
			group: 'tests' with: #('default' 'TruffleSqueak-Tests')]