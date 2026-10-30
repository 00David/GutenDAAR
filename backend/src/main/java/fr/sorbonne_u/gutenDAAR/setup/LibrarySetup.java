package fr.sorbonne_u.gutenDAAR.setup;

import fr.sorbonne_u.gutenDAAR.setup.GutenDAARCatalogCSV.GutenDAAREntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LibrarySetup implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LibrarySetup.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<GutenDAAREntry> catalog = GutenDAARCatalogCSV.retrieve();
        LOG.info("Catalog ready: {} book references.", catalog.size());
    }
}
