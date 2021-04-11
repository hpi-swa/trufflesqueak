stepping and presenter
step
	| now mSecs framesPerSec queueSize newContents |
	framesSinceLastDisplay := framesSinceLastDisplay + 1.
	now := Time millisecondClockValue.
	mSecs := now - lastDisplayTime.
	(mSecs > 500 or: [mSecs < 0 "clock wrap-around"]) ifTrue: 
		[framesPerSec := (framesSinceLastDisplay * 1000) // mSecs.
		queueSize := Graal runtime ifNil: [ 'n/a' ] ifNotNil: [ :r | r getCompilationQueueSize asNumber ].
		newContents := 'Frame rate: ', framesPerSec printString, ' fps / Graal queue: ', queueSize.
		self contents: newContents.
		FileStream stdout nextPutAll: newContents; cr; flush.
		lastDisplayTime := now.
		framesSinceLastDisplay := 0]
	ifFalse:
		["Ensure at least one pixel is drawn per frame"
		Preferences higherPerformance ifTrue: [self invalidRect: (self position extent: 1@1)]]