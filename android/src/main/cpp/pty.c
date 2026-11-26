/*
 * TERMYX PTY helper (own implementation).
 *
 * Spawns a command inside a pseudo-terminal (PTY) using the standard POSIX
 * technique: open /dev/ptmx, grantpt/unlockpt/ptsname_r, fork(), setsid(),
 * dup2() the slave onto stdin/stdout/stderr and execvp() the command. This is
 * the same approach Termux uses, reimplemented here as original code so the
 * TERMYX terminal has a real tty (TERM, job control, signals, full-screen apps).
 */

#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

static int throw_runtime_exception(JNIEnv* env, const char* message) {
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (exClass == NULL) return -1;
    (*env)->ThrowNew(env, exClass, message);
    return -1;
}

static int create_subprocess(JNIEnv* env,
        const char* cmd,
        const char* cwd,
        char* const argv[],
        char** envp,
        int* pProcessId,
        jint rows,
        jint columns,
        jint cell_width,
        jint cell_height) {
    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) return throw_runtime_exception(env, "Cannot open /dev/ptmx");

    char devname[64];
    if (grantpt(ptm) || unlockpt(ptm) || ptsname_r(ptm, devname, sizeof(devname)) != 0) {
        return throw_runtime_exception(env, "Cannot grantpt()/unlockpt()/ptsname_r() on /dev/ptmx");
    }

    // Enable UTF-8 mode and disable flow control so Ctrl+S does not lock the display.
    struct termios tios;
    if (tcgetattr(ptm, &tios) == 0) {
        tios.c_iflag |= IUTF8;
        tios.c_iflag &= ~(IXON | IXOFF);
        tcsetattr(ptm, TCSANOW, &tios);
    }

    struct winsize sz = {
        .ws_row = (unsigned short) rows,
        .ws_col = (unsigned short) columns,
        .ws_xpixel = (unsigned short) (columns * (cell_width > 0 ? cell_width : 8)),
        .ws_ypixel = (unsigned short) (rows * (cell_height > 0 ? cell_height : 16))
    };
    ioctl(ptm, TIOCSWINSZ, &sz);

    pid_t pid = fork();
    if (pid < 0) {
        return throw_runtime_exception(env, "Fork failed");
    } else if (pid > 0) {
        *pProcessId = (int) pid;
        return ptm;
    } else {
        sigset_t signals_to_unblock;
        sigfillset(&signals_to_unblock);
        sigprocmask(SIG_UNBLOCK, &signals_to_unblock, 0);

        close(ptm);
        setsid();

        int pts = open(devname, O_RDWR);
        if (pts < 0) _exit(1);

        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);

        DIR* self_dir = opendir("/proc/self/fd");
        if (self_dir != NULL) {
            int self_dir_fd = dirfd(self_dir);
            struct dirent* entry;
            while ((entry = readdir(self_dir)) != NULL) {
                int fd = atoi(entry->d_name);
                if (fd > 2 && fd != self_dir_fd) close(fd);
            }
            closedir(self_dir);
        }

        clearenv();
        if (envp) for (; *envp; ++envp) putenv(*envp);

        if (cwd != NULL && chdir(cwd) != 0) {
            char* error_message;
            if (asprintf(&error_message, "chdir(\"%s\")", cwd) == -1) error_message = "chdir()";
            perror(error_message);
            fflush(stderr);
        }
        execvp(cmd, argv);
        char* error_message;
        if (asprintf(&error_message, "exec(\"%s\")", cmd) == -1) error_message = "exec()";
        perror(error_message);
        _exit(1);
    }
}

JNIEXPORT jintArray JNICALL Java_com_termx_terminal_Pty_nativeCreateSubprocess(
        JNIEnv* env,
        jclass clazz,
        jstring cmd,
        jstring cwd,
        jobjectArray args,
        jobjectArray envVars,
        jint rows,
        jint columns,
        jint cell_width,
        jint cell_height) {
    (void) clazz;

    jsize size = args ? (*env)->GetArrayLength(env, args) : 0;
    char** argv = NULL;
    if (size > 0) {
        argv = (char**) malloc((size + 1) * sizeof(char*));
        if (!argv) return NULL;
        for (int i = 0; i < size; ++i) {
            jstring arg_java = (jstring) (*env)->GetObjectArrayElement(env, args, i);
            const char* arg_utf8 = (*env)->GetStringUTFChars(env, arg_java, NULL);
            argv[i] = strdup(arg_utf8);
            (*env)->ReleaseStringUTFChars(env, arg_java, arg_utf8);
        }
        argv[size] = NULL;
    }

    size = envVars ? (*env)->GetArrayLength(env, envVars) : 0;
    char** envp = NULL;
    if (size > 0) {
        envp = (char**) malloc((size + 1) * sizeof(char*));
        if (!envp) return NULL;
        for (int i = 0; i < size; ++i) {
            jstring env_java = (jstring) (*env)->GetObjectArrayElement(env, envVars, i);
            const char* env_utf8 = (*env)->GetStringUTFChars(env, env_java, 0);
            envp[i] = strdup(env_utf8);
            (*env)->ReleaseStringUTFChars(env, env_java, env_utf8);
        }
        envp[size] = NULL;
    }

    int procId = 0;
    const char* cmd_cwd = (*env)->GetStringUTFChars(env, cwd, NULL);
    const char* cmd_utf8 = (*env)->GetStringUTFChars(env, cmd, NULL);
    int ptm = create_subprocess(env, cmd_utf8, cmd_cwd, argv, envp, &procId,
                                rows, columns, cell_width, cell_height);
    (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd);
    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);

    if (argv) {
        for (char** tmp = argv; *tmp; ++tmp) free(*tmp);
        free(argv);
    }
    if (envp) {
        for (char** tmp = envp; *tmp; ++tmp) free(*tmp);
        free(envp);
    }

    if (ptm < 0) return NULL;

    jintArray result = (*env)->NewIntArray(env, 2);
    if (!result) return NULL;
    jint vals[2] = { ptm, procId };
    (*env)->SetIntArrayRegion(env, result, 0, 2, vals);
    return result;
}

JNIEXPORT void JNICALL Java_com_termx_terminal_Pty_nativeSetWindowSize(
        JNIEnv* env, jclass clazz, jint fd, jint rows, jint cols, jint cell_width, jint cell_height) {
    (void) env; (void) clazz;
    struct winsize sz = {
        .ws_row = (unsigned short) rows,
        .ws_col = (unsigned short) cols,
        .ws_xpixel = (unsigned short) (cols * (cell_width > 0 ? cell_width : 8)),
        .ws_ypixel = (unsigned short) (rows * (cell_height > 0 ? cell_height : 16))
    };
    ioctl(fd, TIOCSWINSZ, &sz);
}

JNIEXPORT jint JNICALL Java_com_termx_terminal_Pty_nativeWaitFor(
        JNIEnv* env, jclass clazz, jint pid) {
    (void) env; (void) clazz;
    int status;
    if (waitpid(pid, &status, 0) < 0) return -1;
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    if (WIFSIGNALED(status)) return -WTERMSIG(status);
    return 0;
}

JNIEXPORT jint JNICALL Java_com_termx_terminal_Pty_nativeRead(
        JNIEnv* env, jclass clazz, jint fd, jbyteArray buf, jint len) {
    (void) clazz;
    jbyte* out = (*env)->GetByteArrayElements(env, buf, NULL);
    if (!out) return -1;
    ssize_t n = read(fd, out, (size_t) len);
    (*env)->ReleaseByteArrayElements(env, buf, out, 0);
    return (jint) n;
}

JNIEXPORT jint JNICALL Java_com_termx_terminal_Pty_nativeWrite(
        JNIEnv* env, jclass clazz, jint fd, jbyteArray buf, jint len) {
    (void) clazz;
    jbyte* in = (*env)->GetByteArrayElements(env, buf, NULL);
    if (!in) return -1;
    ssize_t n = write(fd, in, (size_t) len);
    (*env)->ReleaseByteArrayElements(env, buf, in, JNI_ABORT);
    return (jint) n;
}

JNIEXPORT void JNICALL Java_com_termx_terminal_Pty_nativeClose(
        JNIEnv* env, jclass clazz, jint fileDescriptor) {
    (void) env; (void) clazz;
    close(fileDescriptor);
}
