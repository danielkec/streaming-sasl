package me.kec.se.bare;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.spi.LoginModule;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(org.apache.kafka.common.security.plain.PlainLoginModule.class)
final class PlainLoginModuleSubstitution implements LoginModule {

    @Substitute
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        String username = (String) options.get("username");
        if (username != null) {
            subject.getPublicCredentials().add(username);
        }
        String password = (String) options.get("password");
        if (password != null) {
            subject.getPrivateCredentials().add(password);
        }

        System.out.println("REGISTERING subject "+subject);
        //register subject under com.oracle.svm.core.jdk.Target_java_security_AccessController.AccessControllerUtil.NO_CONTEXT_SINGLETON
//        Subject.doAsPrivileged(subject, new PrivilegedAction<Object>() {
//            @Override
//            public Object run() {
//                return null;
//            }
//        }, AccessController.getContext());
    }

    @Substitute
    @Override
    public boolean login() {
        return true;
    }

    @Substitute
    @Override
    public boolean logout() {
        return true;
    }

    @Substitute
    @Override
    public boolean commit() {
        return true;
    }

    @Substitute
    @Override
    public boolean abort() {
        return false;
    }
}
