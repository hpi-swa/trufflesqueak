import os
import mx


def squeak(args, extra_vm_args=None, env=None, jdk=None, **kwargs):
    env = env if env else os.environ

    vm_args, squeak_args = mx.extract_VM_args(
        args, useDoubleDash=False, defaultAllVMArgs=False)

    classpath = ["de.hpi.swa.trufflesqueak"]
    USES_GRAAL = mx.suite("compiler", fatalIfMissing=False)
    vm_args = ['-cp', mx.classpath(classpath)]

    if not jdk:
        jdk = mx.get_jdk(tag='jvmci')

    vm_args += [
        '-Dgraal.TraceTruffleCompilation=true',
        # '-Dgraal.Dump=',
        # '-Dgraal.MethodFilter=Truffle.*',
        # '-XX:CompileCommand=print,*OptimizedCallTarget.callRoot',
        # '-XX:CompileCommand=exclude,*OptimizedCallTarget.callRoot',
        # '-Dgraal.TruffleBackgroundCompilation=false',
        # '-Dgraal.TraceTrufflePerformanceWarnings=true',
        # '-Dgraal.TruffleCompilationExceptionsArePrinted=true',
    ]

    if USES_GRAAL:
        graal_path = mx.classpath('compiler:GRAAL', jdk=jdk)
        vm_args += [
            '-XX:+UseJVMCICompiler',
            '-Djvmci.Compiler=graal',
            '-Djvmci.class.path.append=%s' % graal_path
        ]

    # default: assertion checking is enabled
    if extra_vm_args is None or '-da' not in extra_vm_args:
        vm_args += ['-ea', '-esa']

    if extra_vm_args:
        vm_args += extra_vm_args

    vm_args.append("de.hpi.swa.trufflesqueak.TruffleSqueakMain")
    return mx.run_java(vm_args + squeak_args, jdk=jdk, **kwargs)


mx.update_commands(mx.suite('trufflesqueak'), {
    'squeak': [squeak, '[Squeak args|@VM options]'],
})
