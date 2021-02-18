/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package me.kec.se.bare;

import java.security.AccessController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.spi.LoginModule;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.KeepOriginal;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.util.ReflectionUtil;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.auth.SaslExtensions;
import org.apache.kafka.common.security.auth.SaslExtensionsCallback;
import org.apache.kafka.common.security.authenticator.LoginManager;
import org.apache.kafka.common.security.plain.internals.PlainSaslServerProvider;
import org.apache.kafka.common.security.scram.ScramExtensionsCallback;
import org.apache.kafka.common.security.scram.internals.ScramMechanism;
import org.apache.kafka.common.utils.Utils;

@TargetClass(org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler.class)
final class SaslClientCallbackHandlerSubstitution implements AuthenticateCallbackHandler {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.None)
    String mechanism;

//    @Inject
//    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.None)
//    Subject subject;
//
//    @Inject
//    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.None)
//    Map<String, ?> sharedState;

    @Substitute
    public SaslClientCallbackHandlerSubstitution() {
    }

    @Override
    @Substitute
    public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        this.mechanism = saslMechanism;
        PlainSaslServerProvider.initialize();
        System.out.println("Configuring " + saslMechanism);
//        for (AppConfigurationEntry entry : jaasConfigEntries) {
//            System.out.println(jaasConfigEntries.toArray());
//            try {
//                if (subject == null) {
//                    subject = new Subject();
//                }
//                if (sharedState == null) {
//                    sharedState = new HashMap<>();
//                }
//                LoginModule result = Utils.newInstance(entry.getLoginModuleName(), LoginModule.class);
//                result.initialize(subject, this, sharedState, entry.getOptions());
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        }

        // static configs (broker or client)
//        private static final Map<LoginManager.LoginMetadata<String>, LoginManager> STATIC_INSTANCES = new HashMap<>();

        // dynamic configs (broker or client)
//        private static final Map<LoginManager.LoginMetadata<Password>, LoginManager> DYNAMIC_INSTANCES = new HashMap<>();

        // final Map<Object, LoginManager>  = ReflectionUtil.readStaticField(LoginManager.class, "STATIC_INSTANCES");
        // new LoginMetadata<>(jaasContext.name(), loginClass, loginCallbackClass);
       // Reflection.
//        Subject subject = Subject.getSubject(AccessController.getContext());
//        try {
//            for (AppConfigurationEntry entry : jaasConfigEntries) {
//                LoginModule result = Utils.newInstance(entry.getLoginModuleName(), LoginModule.class);
//                result.initialize(subject, this, Map.of(), entry.getOptions());
//            }
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    @Substitute
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        System.out.println("Handling " + callbacks.length);
        System.out.println("Mechanism " + mechanism);


//        Subject subject = Subject.getSubject(acc);
        //LoginManager.acquireLoginManager(JaasContext.loadClientContext());
        Subject subject = Subject.getSubject(AccessController.getContext());
        System.out.println("Subject: " + subject);
        System.out.println("ac: " + AccessController.getContext().getClass().getName());
        for (Callback callback : callbacks) {
            System.out.println("Callback " + callback.getClass());
            if (callback instanceof NameCallback) {
                NameCallback nc = (NameCallback) callback;
                if (subject != null && !subject.getPublicCredentials(String.class).isEmpty()) {
                    System.out.println("Name subject " + subject.getPublicCredentials(String.class).iterator().next());
                    nc.setName(subject.getPublicCredentials(String.class).iterator().next());
                } else {
                    System.out.println("Name " + nc.getDefaultName());
                    nc.setName(nc.getDefaultName());
                }
            } else if (callback instanceof PasswordCallback) {
                if (subject != null && !subject.getPrivateCredentials(String.class).isEmpty()) {
                    char[] password = subject.getPrivateCredentials(String.class).iterator().next().toCharArray();
                    System.out.println("Pass subject " + new String(password));
                    ((PasswordCallback) callback).setPassword(password);
                } else {
                    String errorMessage = "Could not login: the client is being asked for a password, but the Kafka" +
                            " client code does not currently support obtaining a password from the user.";
                    System.out.println("Pass subject " + errorMessage);
                    throw new UnsupportedCallbackException(callback, errorMessage);
                }
            } else if (callback instanceof RealmCallback) {
                RealmCallback rc = (RealmCallback) callback;
                rc.setText(rc.getDefaultText());
            } else if (callback instanceof AuthorizeCallback) {
                AuthorizeCallback ac = (AuthorizeCallback) callback;
                String authId = ac.getAuthenticationID();
                String authzId = ac.getAuthorizationID();
                ac.setAuthorized(authId.equals(authzId));
                if (ac.isAuthorized()) {
                    ac.setAuthorizedID(authzId);
                }
            } else if (callback instanceof ScramExtensionsCallback) {
                if (ScramMechanism.isScram(mechanism) && subject != null && !subject.getPublicCredentials(Map.class).isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> extensions = (Map<String, String>) subject.getPublicCredentials(Map.class).iterator().next();
                    ((ScramExtensionsCallback) callback).extensions(extensions);
                }
            } else if (callback instanceof SaslExtensionsCallback) {
                if (!SaslConfigs.GSSAPI_MECHANISM.equals(mechanism) &&
                        subject != null && !subject.getPublicCredentials(SaslExtensions.class).isEmpty()) {
                    SaslExtensions extensions = subject.getPublicCredentials(SaslExtensions.class).iterator().next();
                    ((SaslExtensionsCallback) callback).extensions(extensions);
                }
            } else {
                throw new UnsupportedCallbackException(callback, "Unrecognized SASL ClientCallback");
            }
        }
    }

    @Override
    @Substitute
    @KeepOriginal
    public void close() {
    }
}
