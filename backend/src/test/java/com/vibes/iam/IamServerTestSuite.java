package com.vibes.iam;

import com.vibes.iam.controller.AuthControllerIntegrationTest;
import com.vibes.iam.controller.UserControllerIntegrationTest;
import com.vibes.iam.security.JwtUtilTest;
import com.vibes.iam.security.SecurityTest;
import com.vibes.iam.service.AuthServiceTest;
import com.vibes.iam.service.PasswordPolicyServiceTest;
import com.vibes.iam.service.UserServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
    // Service Tests
    UserServiceTest.class,
    AuthServiceTest.class,
    PasswordPolicyServiceTest.class,
    
    // Security Tests
    JwtUtilTest.class,
    SecurityTest.class,
    
    // Integration Tests
    AuthControllerIntegrationTest.class,
    UserControllerIntegrationTest.class
})
public class IamServerTestSuite {
    // Test suite class - no implementation needed
}