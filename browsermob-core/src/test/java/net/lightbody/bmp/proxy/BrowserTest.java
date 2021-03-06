package net.lightbody.bmp.proxy;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.test.util.ProxyServerTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * Tests which require a web browser should be placed in this class so they can be properly configured/ignored for CI builds.
 */
public class BrowserTest extends ProxyServerTest {
    @Test
    public void testCaptureHarHttpsPageWithFirefox() throws Exception {
        WebDriver driver = null;
        try {
            proxy.setCaptureHeaders(true);
            proxy.setCaptureContent(true);

            // get the selenium proxy object
            Proxy seleniumProxy = proxy.seleniumProxy();
            DesiredCapabilities capabilities = new DesiredCapabilities();

            capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

            // start the browser up
            driver = new FirefoxDriver(capabilities);

            proxy.newHar("Google.ca");

            driver.get("https://www.google.ca/");

            // get the HAR data
            Har har = proxy.getHar();

            // make sure something came back in the har
            Assert.assertTrue(!har.getLog().getEntries().isEmpty());

            // show that we can capture the HTML of the root page
            // NOTE: firefox seems to occasionally make its first request to some mozilla address, so we can't rely on getEntries().get(0) to get the actual Google page
            boolean foundGooglePage = false;
            for (HarEntry entry : har.getLog().getEntries()) {
                if (entry.getResponse() != null && entry.getResponse().getContent() != null && entry.getResponse().getContent().getText() != null) {
                    String text = entry.getResponse().getContent().getText();
                    if (text.contains("<title>Google</title>")) {
                        foundGooglePage = true;
                        break;
                    }
                }
            }

            Assert.assertTrue("Did not find any HAR entry containing the text <title>Google</title>", foundGooglePage);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    @Test
    public void testProxyConfigurationThroughFirefoxProfile() {
        WebDriver driver = null;

        try {
            FirefoxProfile profile = new FirefoxProfile();
            profile.setAcceptUntrustedCertificates(true);
            profile.setAssumeUntrustedCertificateIssuer(true);
            profile.setPreference("network.proxy.http", "localhost");
            profile.setPreference("network.proxy.http_port", proxy.getPort());
            profile.setPreference("network.proxy.ssl", "localhost");
            profile.setPreference("network.proxy.ssl_port", proxy.getPort());
            profile.setPreference("network.proxy.type", 1);
            profile.setPreference("network.proxy.no_proxies_on", "");

            DesiredCapabilities capabilities = new DesiredCapabilities();

            capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
            capabilities.setCapability(FirefoxDriver.PROFILE, profile);
            capabilities.setCapability(CapabilityType.PROXY,
                    proxy.seleniumProxy());

            driver = new FirefoxDriver(capabilities);
            driver.get("https://www.gmail.com/");
        } finally {
            if (driver != null) {
                driver.close();
            }
        }
    }
}
