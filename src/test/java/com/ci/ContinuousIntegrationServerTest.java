package com.ci;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;


public class ContinuousIntegrationServerTest {

        ContinuousIntegrationServer DEFAULT = new ContinuousIntegrationServer("");
        //Example: Typical test syntax
        @Test
        public void verifyNoExceptionsThrown(){
            DEFAULT.gradleTest();
        }
}
