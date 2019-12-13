baseline
baseline: spec
	<baseline>

	spec for: #'common' do: [
		spec
			package: 'GraalSqueak-Core';
			package: 'GraalSqueak-Support' with: [spec requires: #('GraalSqueak-Core')];
			package: 'GraalSqueak-Tests' with: [spec requires: #('GraalSqueak-Core' 'GraalSqueak-Support')];
			package: 'GraalSqueak-Tools' with: [spec requires: #('GraalSqueak-Core' 'GraalSqueak-Support' 'JSON')];
			package: 'JSON' with: [spec repository: 'http://www.squeaksource.com/JSON'].
		spec
			group: 'default' with: #('GraalSqueak-Core' 'GraalSqueak-Support' 'GraalSqueak-Tools' 'JSON');
			group: 'tests' with: #('default' 'GraalSqueak-Tests')]