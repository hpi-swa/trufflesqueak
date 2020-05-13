baseline
baseline: spec
	<baseline>

	spec for: #'common' do: [ | optionalJSON optionalFileSystem optionalSquot |
		"Hack: depend on external packages only if not present to avoid updates (quite slow on TruffleSqueak)"
		optionalJSON := optionalFileSystem := optionalSquot := #().
		Smalltalk at: #Json ifAbsent: [
			optionalJSON := #('JSON').
			spec package: 'JSON' with: [spec repository: 'http://www.squeaksource.com/JSON']].
		Smalltalk at: #FileSystem ifAbsent: [
			optionalFileSystem := #('FileSystem').
			spec baseline: 'FileSystem' with: [
				spec
					repository: 'github://j4yk/Squeak-FileSystem:master/src';
					loads: #('default')]].
		Smalltalk at: #GitFilesystem ifAbsent: [
			optionalSquot := #('Squot').
			spec baseline: 'Squot' with: [
				spec
					repository: 'github://hpi-swa/Squot:master/src';
					loads: #('FileSystemGit')]].

		spec
			package: 'TruffleSqueak-Core';
			package: 'TruffleSqueak-Support' with: [spec requires: #('TruffleSqueak-Core')];
			package: 'TruffleSqueak-Tests' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Support' 'TruffleSqueak-Utilities')];
			package: 'TruffleSqueak-Tools-Core' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Tools-Support')];
			package: 'TruffleSqueak-Tools-Editor-Core' with: [spec requires: #('TruffleSqueak-Tools-Editor-Support'
				'TruffleSqueak-Tools-Core' "for PolyglotWorkspace extensions"
				'TruffleSqueak-Tools-Notebook-Core' "for PolyglotNotebook extensions"
					), optionalJSON, optionalFileSystem, optionalSquot];
			package: 'TruffleSqueak-Tools-Editor-Support' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Tools-Support')];
			package: 'TruffleSqueak-Tools-Editor-Tests' with: [spec requires: #('TruffleSqueak-Tools-Editor-Core' 'TruffleSqueak-Tools-Editor-Support')];
			package: 'TruffleSqueak-Tools-Notebook-Core' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Utilities'), optionalJSON];
			package: 'TruffleSqueak-Tools-Support' with: [spec requires: #('TruffleSqueak-Core')];
			package: 'TruffleSqueak-Utilities' with: [spec requires: #('TruffleSqueak-Core' 'TruffleSqueak-Support')].

		spec
			group: 'core' with: #('TruffleSqueak-Core');
			group: 'default' with: #('TruffleSqueak-Core' 'TruffleSqueak-Support' 'TruffleSqueak-Tools-Core'
									'TruffleSqueak-Tools-Editor-Core' 'TruffleSqueak-Tools-Notebook-Core' 'TruffleSqueak-Utilities');
			group: 'tests' with: #('default' 'TruffleSqueak-Tests')]