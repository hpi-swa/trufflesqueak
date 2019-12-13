baseline
baseline: spec
	<baseline>

	spec for: #'common' do: [ | needsJSON |
		"Hack: depend on JSON only if not present to avoid updates (quite slow on GraalSqueak)"
		needsJSON := (Smalltalk at: #Json ifAbsent: [nil]) isNil.
		spec
			package: 'GraalSqueak-Core';
			package: 'GraalSqueak-Support' with: [spec requires: #('GraalSqueak-Core')];
			package: 'GraalSqueak-Tests' with: [spec requires: #('GraalSqueak-Core' 'GraalSqueak-Support')];
			package: 'GraalSqueak-Tools' with: [spec requires: #('GraalSqueak-Core' 'GraalSqueak-Support'),
				(needsJSON ifTrue: [#('JSON')] ifFalse: [#()])].
		needsJSON ifTrue: [
			spec package: 'JSON' with: [spec repository: 'http://www.squeaksource.com/JSON']].
		spec
			group: 'default' with: #('GraalSqueak-Core' 'GraalSqueak-Support' 'GraalSqueak-Tools');
			group: 'tests' with: #('default' 'GraalSqueak-Tests')]