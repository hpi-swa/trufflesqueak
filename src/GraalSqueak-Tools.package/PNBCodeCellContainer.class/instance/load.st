actions
load

	| fileChooser stream |
	fileChooser := FileChooser new.
	fileChooser initializeAsSystemWindowWithCaptionPane.
	fileChooser setCaption: 'Select a Juypter notebook file'.
	fileChooser setSuffixes: {'ipynb'}.
	stream := fileChooser open.
	stream ifNotNil: [self loadFromJson: stream]