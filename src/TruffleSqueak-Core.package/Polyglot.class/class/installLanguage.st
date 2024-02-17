helpers
installLanguage
	| downloader modulesPath selectedLanguage version |
	[ downloader := Java type: 'org.graalvm.maven.downloader.Main' ]
		on: Error
		do: [ self error: 'Unable to find Maven Downloader. Additional languages can only be installed in a JVM standalone of TruffleSqueak.'].
	modulesPath := (FSLocator vmDirectory resolve: '../modules').
	modulesPath isDirectory ifFalse: [ self error: 'Unable to find modules/ directory.' ].
	selectedLanguage := UIManager default 
		chooseFrom: #('JavaScript' 'Ruby' 'Python' 'Java' 'LLVM' 'Wasm')
		values: #('js' 'ruby' 'python' 'java' 'llvm' 'wasm')
		title: 'Which language would you like to install?'.
	version := UIManager default
		request: 'Select a version:'
		initialAnswer: ((Java type: 'java.lang.System') getProperty: 'org.graalvm.version' and: 'unknown').
	'Installing language and its dependencies...' displayProgressFrom: 0 to: 1 during: [ :bar |
		bar value: 0.
		downloader main: {
			'-o'. modulesPath fullName.
			'-v'. version.
			'-a'. selectedLanguage
		}.
	].
	self inform: 'Success! Please restart TruffleSqueak for the language to become available. You can also install more languages now.'