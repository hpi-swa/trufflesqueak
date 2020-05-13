path utils
parentFolderOf: aString

	^ aString copyFrom: 1 to: (aString lastIndexOfAnyOf: '/\') - 1