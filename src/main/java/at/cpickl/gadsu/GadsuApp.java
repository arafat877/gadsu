package at.cpickl.gadsu;

import javax.swing.JFrame;
import javax.swing.UIManager;

import at.cpickl.gadsu.service.LogConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GadsuApp {

    private static final Logger LOG = LoggerFactory.getLogger(GadsuApp.class);

    public static void main(String[] args) {
        if (System.getProperty("gadsu.disableLog", "").equals("true")) {
            System.out.println("Gadsu log configuration disabled. (most likely because tests come with own log config)");
        } else {
            LogConfigurator.INSTANCE.configureLog();
        }

        JFrame.setDefaultLookAndFeelDecorated(true);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // UIManager.setLookAndFeel(new SubstanceRavenLookAndFeel());
        } catch (Exception e) {
            LOG.error("Could not set native look&feel!", e);
        }

        new GadsuStarter().start(args);
    }

}
