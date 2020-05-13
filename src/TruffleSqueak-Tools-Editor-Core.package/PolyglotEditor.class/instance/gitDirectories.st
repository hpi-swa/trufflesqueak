initialize-release
gitDirectories
	Smalltalk at: #SquotWorkingCopy ifPresent: [:workingCopy |
		^ (workingCopy registered	collect: [:each | [ | gitTree |
			gitTree := each repository gitRepository repository head tree 
				asTreeEntryNamed: 'Git: ', each asString.
			PEFileEntryGitDirectory on: gitTree path: FSRelativePath new] ifError: [nil]]
		thenSelect: #notNil)].
	^ {}