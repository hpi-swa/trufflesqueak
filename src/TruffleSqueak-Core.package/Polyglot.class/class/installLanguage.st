helpers
installLanguage
	| modulesPath languageId version |
	modulesPath := (FSLocator vmDirectory resolve: '../modules').
	modulesPath isDirectory ifFalse: [ self error: 'Unable to find modules/ directory.' ].
	languageId := UIManager default 
		chooseFrom: #('JavaScript' 'Ruby' 'Python' 'Java' 'LLVM' 'Wasm')
		values: #('js' 'ruby' 'python' 'java' 'llvm' 'wasm')
		title: 'Which language would you like to install?'.
	version := UIManager default
		request: 'Select a version:'
		initialAnswer: ((Java type: 'java.lang.System') getProperty: 'org.graalvm.version' and: 'unknown').
	self installLanguage: languageId version: version path: modulesPath fullName