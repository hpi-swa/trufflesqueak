helpers
installLanguage: languageId version: version path: path
	| downloader |
	[ downloader := Java type: 'org.graalvm.maven.downloader.Main' ]
		on: Error
		do: [ self error: 'Unable to find Maven Downloader. Additional languages can only be installed in a JVM standalone of TruffleSqueak.' ].
	downloader main: {
		'-o'. path.
		'-v'. version.
		'-a'. languageId
	}