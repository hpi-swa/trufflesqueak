[[[ | mc |
mc := MCFileTreeRepository path: '%s'.
Installer monticello
    mc: mc;
    packages: mc allPackageNames;
    install ]
        on: Warning do: [ :w | w resume ]]
        on: Error do: [ :e | e retry ]]
        on: ProgressInitiationException do: [ :e |
            e isNested
                ifTrue: [ e pass ]
                ifFalse: [ e rearmHandlerDuring:
                    [[ e sendNotificationsTo: [ :min :max :current | "silence" ]]
                        on: ProgressNotification do: [ :notification | notification resume ]]]].

Smalltalk snapshot: true andQuit: false.
