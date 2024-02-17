helpers
installLanguage: languageId version: version path: path
	| downloader |
	[ downloader := Java type: 'org.graalvm.maven.downloader.Main' ]
		on: Error
		do: [ self error: 'Unable to find Maven Downloader. Additional languages can only be installed in a JVM standalone of TruffleSqueak.'].
	'Installing language and its dependencies...' displayProgressFrom: 0 to: 1 during: [ :bar |
		bar value: 0.
		downloader main: {
			'-o'. path.
			'-v'. version.
			'-a'. languageId
		}.
	].
	self inform: 'Success! Please restart TruffleSqueak for the language to become available. You can also install more languages now.'