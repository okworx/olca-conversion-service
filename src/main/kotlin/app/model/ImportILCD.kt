package app.model

import org.openlca.core.database.IDatabase
import org.openlca.ilcd.io.ZipStore
import org.openlca.io.ilcd.ILCDImport
import org.openlca.io.ilcd.input.ImportConfig
import app.Server
import org.slf4j.LoggerFactory
import java.net.URL
import java.io.File
import org.openlca.core.database.ImpactCategoryDao
import org.openlca.core.model.ImpactCategory
import de.uba.probas2.util.RestPathUtil

class ImportILCD : Import {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doIt(setup: ConversionSetup, db: IDatabase) {
        val url = setup.url
        if (url.isEmpty() || !url.contains("/processes/"))
            throw Exception("Invalid URL: $url")
       
		// first import the UBA LCIA methods
        val lciamstore = ZipStore(File("UBA_LCIAmethods.zip"))
		
        val lciamconf = ImportConfig(lciamstore, db)
        lciamconf.flowMap = setup.flowMap()
        val lciamimp = ILCDImport(lciamconf)
        lciamimp.run()

		// the original URL looks like https://acme.org/resource/processes/f00b951c-e67f-4f7f-8dd4-665de3974e18?version=00.00.000
		val rpu = RestPathUtil(url);
  		val baseurl = rpu.getBaseUrl()
		val uuid = rpu.getUuid()
		val versionParam = rpu.getVersionParam()
		
		val zipurl = baseurl.plus("/processes/").plus(uuid).plus("/zipexport?").plus(versionParam)
		
		// instead of creating a SodaClient as DatasSource, we'll fetch
		// the dataset including its dependencies as ZIP
        log.debug("calling URL {}", zipurl)
        
		val tempFile = Server.cache!!.tempFile(ext = ".zip")
        log.debug("copy data to {}", tempFile)
        URL(zipurl).openStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

		val store = ZipStore(tempFile)
		
        val conf = ImportConfig(store, db)
        conf.flowMap = setup.flowMap()
        val imp = ILCDImport(conf)
        imp.run()

        log.debug("data imported; delete file {}", tempFile)
        tempFile.delete()
    }

}
